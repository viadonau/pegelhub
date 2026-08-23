#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
. "$SCRIPT_DIR/../../lib/env-file.sh"
COMPOSE_FILE="$DEPLOY_DIR/compose.yaml"
CONFIG_DIR=${PEGELHUB_CONFIG_DIR:-}
ENV_FILE=${PEGELHUB_ENV_FILE:-}
if [ -z "$ENV_FILE" ]; then
  [ -n "$CONFIG_DIR" ] || { printf 'ERROR: Set PEGELHUB_CONFIG_DIR or PEGELHUB_ENV_FILE.\n' >&2; exit 1; }
  ENV_FILE="$CONFIG_DIR/pegelhub.env"
fi
[ -n "$CONFIG_DIR" ] || CONFIG_DIR=$(CDPATH= cd -- "$(dirname -- "$ENV_FILE")" && pwd)
ca_bundle=""

cleanup() {
  [ -z "$ca_bundle" ] || rm -f "$ca_bundle"
}
trap cleanup EXIT HUP INT TERM

fail() {
  printf '%s\n' "ERROR: $*" >&2
  exit 1
}

retry() {
  description="$1"
  shift
  attempts="${SMOKE_RETRIES:-30}"
  delay_seconds="${SMOKE_RETRY_DELAY_SECONDS:-5}"

  i=1
  while [ "$i" -le "$attempts" ]; do
    if "$@"; then
      return 0
    fi

    if [ "$i" -eq "$attempts" ]; then
      fail "$description failed after $attempts attempts."
    fi

    printf '%s\n' "$description not ready yet; retrying in ${delay_seconds}s ($i/$attempts)..."
    sleep "$delay_seconds"
    i=$((i + 1))
  done
}

[ -f "$ENV_FILE" ] || fail "Missing $ENV_FILE."

compose() {
  COMPOSE_PROJECT_NAME="$compose_project_name" \
  COMPOSE_IGNORE_ORPHANS=true \
  COMPOSE_REMOVE_ORPHANS=false \
    docker compose \
      -p "$compose_project_name" \
      --env-file "$ENV_FILE" \
      -f "$COMPOSE_FILE" \
      "$@"
}

api_hostname=$(env_value PEGELHUB_API_HOSTNAME)
frontend_hostname=$(env_value PEGELHUB_FRONTEND_HOSTNAME)
keycloak_hostname=$(env_value PEGELHUB_KEYCLOAK_HOSTNAME)
compose_project_name=$(env_value COMPOSE_PROJECT_NAME)

[ -n "$compose_project_name" ] || fail "COMPOSE_PROJECT_NAME is missing."
[ -n "$frontend_hostname" ] || fail "PEGELHUB_FRONTEND_HOSTNAME is missing."
[ -n "$api_hostname" ] || fail "PEGELHUB_API_HOSTNAME is missing."
[ -n "$keycloak_hostname" ] || fail "PEGELHUB_KEYCLOAK_HOSTNAME is missing."

if [ "$(env_value PEGELHUB_TRUST_MODE)" = "custom" ]; then
  trust_dir=$(env_value PEGELHUB_TRUST_DIR)
  [ -n "$trust_dir" ] || trust_dir="$CONFIG_DIR/tls/trust"
  ca_bundle=$(mktemp "${TMPDIR:-/tmp}/pegelhub-ca-bundle.XXXXXX")
  "$SCRIPT_DIR/build-ca-bundle.sh" "$trust_dir" "$ca_bundle"
  CURL_CA_BUNDLE=$ca_bundle
  export CURL_CA_BUNDLE
fi

FRONTEND_BASE_URL=${FRONTEND_BASE_URL:-https://$frontend_hostname}
API_BASE_URL=${API_BASE_URL:-https://$api_hostname}
KEYCLOAK_ISSUER_URI=${KEYCLOAK_ISSUER_URI:-https://$keycloak_hostname/realms/pegelhub}

unset \
  COMPOSE_PROJECT_NAME \
  PEGELHUB_FRONTEND_HOSTNAME \
  PEGELHUB_API_HOSTNAME \
  PEGELHUB_KEYCLOAK_HOSTNAME \
  PEGELHUB_TLS_MODE \
  PEGELHUB_TRUST_MODE \
  PEGELHUB_TLS_SERVER_DIR \
  PEGELHUB_TRUST_DIR \
  META_PASSWORD \
  META_DB \
  INFLUX_ADMIN_USER \
  INFLUX_ADMIN_PASSWORD \
  INFLUX_ORG \
  INFLUX_INTERNAL_BUCKET \
  INFLUX_TOKEN \
  INFLUX_DATA_BUCKET \
  INFLUX_DATA_RETENTION \
  INFLUX_TELEMETRY_BUCKET \
  INFLUX_TELEMETRY_RETENTION \
  INFLUX_LATEST_RANGE \
  KEYCLOAK_DB_PASSWORD \
  KEYCLOAK_DB \
  KEYCLOAK_ADMIN_USER \
  KEYCLOAK_ADMIN_PASSWORD \
  CORE_JAVA_TOOL_OPTIONS \
  FLYWAY_BASELINE_ON_MIGRATE

for tls_url in "$FRONTEND_BASE_URL" "$API_BASE_URL" "https://$keycloak_hostname"; do
  printf '%s\n' "Checking TLS handshake for $tls_url..."
  retry "TLS handshake for $tls_url" \
    sh -c 'curl -sS -o /dev/null "$1/"' sh "$tls_url"
done

frontend_running() {
  docker ps \
    --filter "label=com.docker.compose.project=$compose_project_name" \
    --filter 'label=com.docker.compose.service=frontend' \
    --filter status=running \
    --format '{{.ID}}' | grep -q .
}

if frontend_running; then
  printf '%s\n' "Checking public frontend route..."
  retry "Public frontend route" sh -c 'curl -fsS "$1/" >/dev/null' sh "$FRONTEND_BASE_URL"
else
  printf '%s\n' "Skipping public frontend route; no frontend container is running."
fi

printf '%s\n' "Checking public API route..."
retry "Public API route" sh -c 'curl -fsS "$1/api/v1/measurements/system-time" >/dev/null' sh "$API_BASE_URL"

printf '%s\n' "Checking public Keycloak issuer discovery..."
retry "Public Keycloak issuer discovery" sh -c 'curl -fsS "$1/.well-known/openid-configuration" | grep -q "\"issuer\""' sh "$KEYCLOAK_ISSUER_URI"

check_core_health() {
  compose exec -T caddy wget -qO- http://core-app:8081/actuator/health \
    | grep -q '"status":"UP"'
}

check_keycloak_health() {
  compose exec -T caddy wget -qO- http://keycloak:9000/health/ready \
    | grep -q '"status"'
}

printf '%s\n' "Checking internal Core actuator health..."
retry "Internal Core actuator health" check_core_health

printf '%s\n' "Checking internal Keycloak management health..."
retry "Internal Keycloak management health" check_keycloak_health

printf '%s\n' "Deployment smoke checks passed."
