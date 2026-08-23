#!/bin/sh

# Proves idempotent service-client provisioning and the resulting access-token
# contract in an isolated disposable Keycloak project.
set -eu

TEST_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$TEST_DIR/.." && pwd)
COMPOSE_FILE="$DEPLOY_DIR/compose.yaml"
ENV_TEMPLATE="$DEPLOY_DIR/pegelhub.env.example"
PROJECT_NAME="pegelhub-keycloak-test-service-$(date +%s)-$$"
TEMP_DIR=$(mktemp -d "${TMPDIR:-/tmp}/$PROJECT_NAME.XXXXXX")
ENV_FILE="$TEMP_DIR/pegelhub.env"
STATE_DIR="$TEMP_DIR/state"
SECRET_FILE="$TEMP_DIR/secrets/iec-client.secret"
FIRST_LOG="$TEMP_DIR/first.log"
SECOND_LOG="$TEMP_DIR/second.log"
TOKEN_RESPONSE="$TEMP_DIR/token.json"
CLIENT_ID=iec-test-client
CLEANUP_ENABLED=false

fail() {
  printf '%s\n' "ERROR: $*" >&2
  exit 1
}

compose() {
  COMPOSE_PROJECT_NAME="$PROJECT_NAME" \
    docker compose \
      -p "$PROJECT_NAME" \
      --env-file "$ENV_FILE" \
      -f "$COMPOSE_FILE" \
      "$@"
}

cleanup() {
  if [ "$CLEANUP_ENABLED" = "true" ]; then
    compose down -v --remove-orphans >/dev/null 2>&1 || true
  fi
  rm -rf "$TEMP_DIR"
}
trap cleanup EXIT HUP INT TERM

command -v docker >/dev/null 2>&1 || fail "docker is required."
command -v jq >/dev/null 2>&1 || fail "jq is required."
command -v openssl >/dev/null 2>&1 || fail "openssl is required."
CLEANUP_ENABLED=true
umask 077

sed \
  -e "s/^COMPOSE_PROJECT_NAME=.*/COMPOSE_PROJECT_NAME=$PROJECT_NAME/" \
  -e 's/^PEGELHUB_FRONTEND_HOSTNAME=.*/PEGELHUB_FRONTEND_HOSTNAME=frontend.keycloak.test/' \
  -e 's/^PEGELHUB_API_HOSTNAME=.*/PEGELHUB_API_HOSTNAME=api.keycloak.test/' \
  -e 's/^PEGELHUB_KEYCLOAK_HOSTNAME=.*/PEGELHUB_KEYCLOAK_HOSTNAME=auth.keycloak.test/' \
  -e "s|^# PEGELHUB_TLS_SERVER_DIR=.*|PEGELHUB_TLS_SERVER_DIR=$TEMP_DIR/tls/server|" \
  -e "s|^# PEGELHUB_TRUST_DIR=.*|PEGELHUB_TRUST_DIR=$TEMP_DIR/tls/trust|" \
  "$ENV_TEMPLATE" > "$ENV_FILE"
mkdir -p "$STATE_DIR" "$TEMP_DIR/tls/server" "$TEMP_DIR/tls/trust"
chmod 600 "$ENV_FILE"

PEGELHUB_CONFIG_DIR="$TEMP_DIR" PEGELHUB_STATE_DIR="$STATE_DIR" PEGELHUB_ENV_FILE="$ENV_FILE" \
  "$DEPLOY_DIR/scripts/init-env-secrets.sh" >/dev/null
PEGELHUB_CONFIG_DIR="$TEMP_DIR" PEGELHUB_STATE_DIR="$STATE_DIR" PEGELHUB_ENV_FILE="$ENV_FILE" \
  "$DEPLOY_DIR/scripts/bootstrap-keycloak.sh" >/dev/null

if ! PEGELHUB_CONFIG_DIR="$TEMP_DIR" PEGELHUB_STATE_DIR="$STATE_DIR" PEGELHUB_ENV_FILE="$ENV_FILE" \
  "$DEPLOY_DIR/scripts/provision-service-client.sh" \
    "$CLIENT_ID" "$SECRET_FILE" metadata:read measurement:write \
    > "$FIRST_LOG" 2>&1; then
  sed -n '1,120p' "$FIRST_LOG" >&2
  fail "Initial service-client provisioning failed."
fi

secret_mode=$(stat -c '%a' "$SECRET_FILE" 2>/dev/null || stat -f '%Lp' "$SECRET_FILE")
[ "$secret_mode" = "600" ] \
  || fail "The generated client secret is not mode 600."
secret_digest=$(openssl dgst -sha256 "$SECRET_FILE")

if ! PEGELHUB_CONFIG_DIR="$TEMP_DIR" PEGELHUB_STATE_DIR="$STATE_DIR" PEGELHUB_ENV_FILE="$ENV_FILE" \
  "$DEPLOY_DIR/scripts/provision-service-client.sh" \
    "$CLIENT_ID" "$SECRET_FILE" metadata:read measurement:write \
    > "$SECOND_LOG" 2>&1; then
  sed -n '1,120p' "$SECOND_LOG" >&2
  fail "Repeated service-client provisioning failed."
fi

[ "$(openssl dgst -sha256 "$SECRET_FILE")" = "$secret_digest" ] \
  || fail "Repeated provisioning rotated the client credential."
secret_value=$(cat "$SECRET_FILE")
if grep -F "$secret_value" "$FIRST_LOG" "$SECOND_LOG" >/dev/null 2>&1; then
  fail "The service-client provisioner printed a client secret."
fi
unset secret_value

network_id=$(docker network ls \
  --filter "label=com.docker.compose.project=$PROJECT_NAME" \
  --format '{{.ID}}' | head -n 1)
[ -n "$network_id" ] || fail "The disposable Keycloak network is missing."

docker run --rm \
  --network "$network_id" \
  --volume "$SECRET_FILE:/run/client-secret:ro" \
  curlimages/curl:8.12.1 \
  -fsS \
  --data-urlencode grant_type=client_credentials \
  --data-urlencode "client_id=$CLIENT_ID" \
  --data-urlencode client_secret@/run/client-secret \
  http://keycloak:8080/realms/pegelhub/protocol/openid-connect/token \
  > "$TOKEN_RESPONSE"

access_token=$(jq -er '.access_token' "$TOKEN_RESPONSE")
payload=$(printf '%s' "$access_token" | cut -d. -f2 | tr '_-' '/+')
case $((${#payload} % 4)) in
  0) ;;
  2) payload="${payload}==" ;;
  3) payload="${payload}=" ;;
  *) fail "The access token has invalid base64url padding." ;;
esac
claims=$(printf '%s' "$payload" | openssl base64 -d -A)
unset access_token payload

if ! printf '%s\n' "$claims" | jq -e '
  .pegelhub_actor_type == "CLIENT"
  and ((.aud == "pegelhub-core-api") or (((.aud | type) == "array") and (.aud | index("pegelhub-core-api") != null)))
  and ((.resource_access["pegelhub-core-api"].roles // []) | sort)
    == ["measurement:write", "metadata:read"]
' >/dev/null; then
  printf '%s\n' "$claims" | jq '{aud, pegelhub_actor_type, resource_access}' >&2
  fail "The service-client token has incorrect claims."
fi

client_count=$(compose exec -T keycloak sh -eu -c '
  export KC_CLI_PASSWORD="$KC_BOOTSTRAP_ADMIN_PASSWORD"
  /opt/keycloak/bin/kcadm.sh config credentials \
    --config /tmp/service-client-count.config \
    --server http://localhost:8080 --realm master \
    --user "$KC_BOOTSTRAP_ADMIN_USERNAME" >/dev/null 2>&1
  /opt/keycloak/bin/kcadm.sh get clients -r pegelhub \
    -q "clientId=$1" --fields id --config /tmp/service-client-count.config
  rm -f /tmp/service-client-count.config
' sh "$CLIENT_ID" | jq 'length')
[ "$client_count" -eq 1 ] || fail "Repeated provisioning duplicated the client."

printf '%s\n' "Service-client token and idempotency checks passed."
