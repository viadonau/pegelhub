#!/bin/sh

# Runtime verification of the fresh-realm lifecycle in an isolated disposable
# Compose project. It imports and inspects the staging realm, proves a repeated
# import and normal Keycloak recreation preserve existing state, and cleans up
# only the uniquely named test project and volumes it created.
set -eu

TEST_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$TEST_DIR/.." && pwd)
COMPOSE_FILE="$DEPLOY_DIR/compose.yaml"
BOOTSTRAP_COMPOSE_FILE="$DEPLOY_DIR/keycloak-bootstrap.compose.yaml"
ENV_TEMPLATE="$DEPLOY_DIR/.env.example"
PROJECT_NAME="pegelhub-keycloak-test-$(date +%s)-$$"
TEMP_DIR=$(mktemp -d "${TMPDIR:-/tmp}/$PROJECT_NAME.XXXXXX")
ENV_FILE="$TEMP_DIR/.env"
BOOTSTRAP_LOG="$TEMP_DIR/bootstrap.log"
REBOOTSTRAP_LOG="$TEMP_DIR/rebootstrap.log"
SECRET_VALUES_FILE="$TEMP_DIR/.secret-values"
REALM_STATE="$TEMP_DIR/realm.json"
CLIENTS_STATE="$TEMP_DIR/clients.json"
USERS_STATE="$TEMP_DIR/users.json"
FRONTEND_STATE="$TEMP_DIR/frontend.json"
ROLES_STATE="$TEMP_DIR/core-roles.json"
SCOPES_STATE="$TEMP_DIR/client-scopes.json"
ROLES_MAPPERS_STATE="$TEMP_DIR/core-roles-mappers.json"
AUDIENCE_MAPPERS_STATE="$TEMP_DIR/core-audience-mappers.json"
ACTOR_MAPPERS_STATE="$TEMP_DIR/client-actor-mappers.json"
TEST_FRONTEND_HOSTNAME=frontend.keycloak.test
TEST_API_HOSTNAME=api.keycloak.test
TEST_KEYCLOAK_HOSTNAME=auth.keycloak.test
CLEANUP_ENABLED=false

fail() {
  printf '%s\n' "ERROR: $*" >&2
  exit 1
}

compose() {
  COMPOSE_PROJECT_NAME="$PROJECT_NAME" \
  COMPOSE_PROFILES=keycloak-bootstrap \
  PEGELHUB_FRONTEND_HOSTNAME="$TEST_FRONTEND_HOSTNAME" \
    docker compose \
      -p "$PROJECT_NAME" \
      --env-file "$ENV_FILE" \
      -f "$COMPOSE_FILE" \
      -f "$BOOTSTRAP_COMPOSE_FILE" \
      --profile keycloak-bootstrap \
      "$@"
}

normal_compose() {
  COMPOSE_PROJECT_NAME="$PROJECT_NAME" \
  COMPOSE_PROFILES= \
    docker compose \
      -p "$PROJECT_NAME" \
      --env-file "$ENV_FILE" \
      -f "$COMPOSE_FILE" \
      "$@"
}

cleanup() {
  case "$PROJECT_NAME" in
    pegelhub-keycloak-test-*) ;;
    *) return ;;
  esac
  if [ "$CLEANUP_ENABLED" = "true" ]; then
    compose down -v --remove-orphans >/dev/null 2>&1 || true
  fi
  rm -rf "$TEMP_DIR"
}

exit_on_signal() {
  exit_status="$1"
  trap - HUP INT TERM
  cleanup
  exit "$exit_status"
}

trap cleanup EXIT
trap 'exit_on_signal 129' HUP
trap 'exit_on_signal 130' INT
trap 'exit_on_signal 143' TERM

assert_local_docker() {
  if [ -n "${DOCKER_CONTEXT:-}" ]; then
    effective_context=$DOCKER_CONTEXT
    docker_endpoint=$(docker context inspect \
      --format '{{(index .Endpoints "docker").Host}}' \
      "$effective_context")
  elif [ -n "${DOCKER_HOST:-}" ]; then
    docker_endpoint=$DOCKER_HOST
  else
    effective_context=$(docker context show)
    docker_endpoint=$(docker context inspect \
      --format '{{(index .Endpoints "docker").Host}}' \
      "$effective_context")
  fi

  case "$docker_endpoint" in
    unix://*|npipe://*|tcp://localhost:*|tcp://127.0.0.1:*) ;;
    *) fail "Disposable Keycloak verification must target a local Docker endpoint." ;;
  esac

  DOCKER_HOST=$docker_endpoint
  export DOCKER_HOST
  unset DOCKER_CONTEXT
}

assert_no_secret_output() {
  for log_file in "$@"; do
    if grep -F -f "$SECRET_VALUES_FILE" "$log_file" >/dev/null 2>&1; then
      fail "A disposable credential was emitted by the staging Keycloak bootstrap."
    fi
  done
}

configure_kcadm() {
  normal_compose exec -T keycloak sh -eu -c '
    export KC_CLI_PASSWORD="$KC_BOOTSTRAP_ADMIN_PASSWORD"
    /opt/keycloak/bin/kcadm.sh config credentials \
      --config /tmp/pegelhub-bootstrap-test.config \
      --server http://localhost:8080 \
      --realm master \
      --user "$KC_BOOTSTRAP_ADMIN_USERNAME" >/dev/null 2>&1
    unset KC_CLI_PASSWORD KC_BOOTSTRAP_ADMIN_PASSWORD
  '
}

kcadm() {
  normal_compose exec -T keycloak \
    /opt/keycloak/bin/kcadm.sh "$@" \
    --config /tmp/pegelhub-bootstrap-test.config
}

single_id() {
  resource="$1"
  query="$2"
  result=$(kcadm get "$resource" -r pegelhub -q "$query" \
    --fields id --format csv --noquotes | tr -d '\r')
  [ -n "$result" ] || fail "Missing Keycloak object for $query."
  [ "$(printf '%s\n' "$result" | wc -l | tr -d ' ')" -eq 1 ] \
    || fail "Expected one Keycloak object for $query."
  printf '%s\n' "$result"
}

inspect_state() {
  configure_kcadm
  core_id=$(single_id clients clientId=pegelhub-core-api)
  frontend_id=$(single_id clients clientId=pegelhub-frontend)

  kcadm get realms/pegelhub \
    --fields realm,enabled,displayName,loginTheme,internationalizationEnabled,supportedLocales,defaultLocale \
    > "$REALM_STATE"
  kcadm get clients -r pegelhub \
    --fields id,clientId,publicClient,serviceAccountsEnabled \
    > "$CLIENTS_STATE"
  kcadm get users -r pegelhub \
    --fields id,username,serviceAccountClientId \
    > "$USERS_STATE"
  kcadm get "clients/$frontend_id" -r pegelhub \
    --fields 'clientId,publicClient,rootUrl,redirectUris,webOrigins,attributes(*)' \
    > "$FRONTEND_STATE"
  kcadm get "clients/$core_id/roles" -r pegelhub \
    --fields name,composite \
    > "$ROLES_STATE"
  kcadm get client-scopes -r pegelhub --fields id,name > "$SCOPES_STATE"

  roles_scope_id=$(jq -r '.[] | select(.name == "pegelhub-core-roles") | .id' "$SCOPES_STATE")
  audience_scope_id=$(jq -r '.[] | select(.name == "pegelhub-core-audience") | .id' "$SCOPES_STATE")
  actor_scope_id=$(jq -r '.[] | select(.name == "pegelhub-client-actor") | .id' "$SCOPES_STATE")
  [ -n "$roles_scope_id" ] && [ -n "$audience_scope_id" ] && [ -n "$actor_scope_id" ] \
    || fail "Required PegelHub client scopes are missing."

  kcadm get "client-scopes/$roles_scope_id/protocol-mappers/models" -r pegelhub \
    > "$ROLES_MAPPERS_STATE"
  kcadm get "client-scopes/$audience_scope_id/protocol-mappers/models" -r pegelhub \
    > "$AUDIENCE_MAPPERS_STATE"
  kcadm get "client-scopes/$actor_scope_id/protocol-mappers/models" -r pegelhub \
    > "$ACTOR_MAPPERS_STATE"
}

assert_seed_state() {
  jq -e '
    .realm == "pegelhub"
    and .enabled == true
    and .displayName == "PegelHub Staging"
    and .loginTheme == "pegelhub"
    and .internationalizationEnabled == true
    and .supportedLocales == ["de"]
    and .defaultLocale == "de"
  ' "$REALM_STATE" >/dev/null \
    || fail "Runtime realm locale or theme state is incorrect."

  jq -e '
    ([.[] | select(.clientId | startswith("pegelhub-")) | .clientId] | sort)
      == ["pegelhub-core-api", "pegelhub-frontend"]
    and ([.[] | select(.clientId | startswith("local-"))] | length) == 0
    and ([.[] | select(.serviceAccountsEnabled == true)] | length) == 0
  ' "$CLIENTS_STATE" >/dev/null \
    || fail "Runtime staging client set is incorrect."

  jq -e 'length == 0' "$USERS_STATE" >/dev/null \
    || fail "The staging seed created a user."

  jq -e '
    .clientId == "pegelhub-frontend"
    and .publicClient == true
    and .rootUrl == "https://frontend.keycloak.test"
    and .redirectUris == ["https://frontend.keycloak.test/*"]
    and .webOrigins == ["https://frontend.keycloak.test"]
    and .attributes["pkce.code.challenge.method"] == "S256"
  ' "$FRONTEND_STATE" >/dev/null \
    || fail "Runtime frontend origin state is incorrect."

  jq -e '
    map({name, composite}) | sort_by(.name) == [
      {"name":"measurement:read","composite":false},
      {"name":"measurement:write","composite":false},
      {"name":"metadata:read","composite":false},
      {"name":"metadata:write","composite":false},
      {"name":"system:admin","composite":false},
      {"name":"telemetry:read","composite":false},
      {"name":"telemetry:write","composite":false}
    ]
  ' "$ROLES_STATE" >/dev/null \
    || fail "Runtime Core API roles are incorrect."

  jq -e '
    [.[] | select(.name == "basic" or .name == "profile" or (.name | startswith("pegelhub-"))) | .name]
    | sort == [
      "basic",
      "pegelhub-client-actor",
      "pegelhub-core-audience",
      "pegelhub-core-roles",
      "pegelhub-user-actor",
      "profile"
    ]
  ' "$SCOPES_STATE" >/dev/null \
    || fail "Runtime PegelHub client scopes are incorrect."

  jq -e '
    length == 1
    and .[0].protocolMapper == "oidc-usermodel-client-role-mapper"
    and .[0].config["claim.name"] == "resource_access.${client_id}.roles"
  ' "$ROLES_MAPPERS_STATE" >/dev/null \
    || fail "Runtime Core role mapper is incorrect."
  jq -e '
    length == 1
    and .[0].protocolMapper == "oidc-audience-mapper"
    and .[0].config["included.client.audience"] == "pegelhub-core-api"
  ' "$AUDIENCE_MAPPERS_STATE" >/dev/null \
    || fail "Runtime Core audience mapper is incorrect."
  jq -e '
    length == 1
    and .[0].protocolMapper == "oidc-hardcoded-claim-mapper"
    and .[0].config["claim.name"] == "pegelhub_actor_type"
    and .[0].config["claim.value"] == "CLIENT"
  ' "$ACTOR_MAPPERS_STATE" >/dev/null \
    || fail "Runtime client actor mapper is incorrect."
}

command -v jq >/dev/null 2>&1 || fail "jq is required."
command -v docker >/dev/null 2>&1 || fail "docker is required."
assert_local_docker
CLEANUP_ENABLED=true
umask 077

sed \
  -e "s/^COMPOSE_PROJECT_NAME=.*/COMPOSE_PROJECT_NAME=$PROJECT_NAME/" \
  -e "s/^PEGELHUB_FRONTEND_HOSTNAME=.*/PEGELHUB_FRONTEND_HOSTNAME=$TEST_FRONTEND_HOSTNAME/" \
  -e "s/^PEGELHUB_API_HOSTNAME=.*/PEGELHUB_API_HOSTNAME=$TEST_API_HOSTNAME/" \
  -e "s/^PEGELHUB_KEYCLOAK_HOSTNAME=.*/PEGELHUB_KEYCLOAK_HOSTNAME=$TEST_KEYCLOAK_HOSTNAME/" \
  "$ENV_TEMPLATE" > "$ENV_FILE"
chmod 600 "$ENV_FILE"
PEGELHUB_STAGING_ENV_FILE="$ENV_FILE" \
  "$DEPLOY_DIR/scripts/init-env-secrets.sh" >/dev/null

awk -F= '
  ($1 == "META_PASSWORD" ||
    $1 == "INFLUX_ADMIN_PASSWORD" ||
    $1 == "INFLUX_TOKEN" ||
    $1 == "KEYCLOAK_DB_PASSWORD" ||
    $1 == "KEYCLOAK_ADMIN_PASSWORD") {
      value = substr($0, length($1) + 2)
      if (length(value) > 0) print value
    }
' "$ENV_FILE" > "$SECRET_VALUES_FILE"
chmod 600 "$SECRET_VALUES_FILE"

if ! PEGELHUB_STAGING_ENV_FILE="$ENV_FILE" \
  PEGELHUB_KEYCLOAK_HOSTNAME=caller-override.invalid \
  "$DEPLOY_DIR/scripts/bootstrap-keycloak.sh" \
  > "$BOOTSTRAP_LOG" 2>&1; then
  grep '^ERROR:' "$BOOTSTRAP_LOG" >&2 || true
  fail "Disposable staging Keycloak bootstrap failed; protected output was withheld."
fi
assert_no_secret_output "$BOOTSTRAP_LOG"
runtime_keycloak_hostname=$(normal_compose exec -T keycloak printenv KC_HOSTNAME | tr -d '\r')
[ "$runtime_keycloak_hostname" = "https://$TEST_KEYCLOAK_HOSTNAME" ] \
  || fail "Caller environment overrode the protected staging Keycloak hostname."
inspect_state
assert_seed_state

if PEGELHUB_STAGING_ENV_FILE="$ENV_FILE" \
  "$DEPLOY_DIR/scripts/bootstrap-keycloak.sh" >/dev/null 2>&1; then
  fail "Offline realm bootstrap did not refuse to run while Keycloak was online."
fi

kcadm create clients -r pegelhub \
  -s clientId=integration-preserved-client \
  -s enabled=false \
  -s publicClient=true >/dev/null 2>&1
normal_compose stop keycloak >/dev/null

if ! PEGELHUB_STAGING_ENV_FILE="$ENV_FILE" \
  "$DEPLOY_DIR/scripts/bootstrap-keycloak.sh" \
  > "$REBOOTSTRAP_LOG" 2>&1; then
  grep '^ERROR:' "$REBOOTSTRAP_LOG" >&2 || true
  fail "Repeated staging Keycloak bootstrap failed; protected output was withheld."
fi
assert_no_secret_output "$REBOOTSTRAP_LOG"
inspect_state
assert_seed_state

marker_count=$(jq '[.[] | select(.clientId == "integration-preserved-client")] | length' \
  "$CLIENTS_STATE")
[ "$marker_count" -eq 1 ] \
  || fail "Repeated bootstrap overwrote or duplicated existing identity state."

normal_compose up -d --wait --force-recreate keycloak >/dev/null
configure_kcadm
marker_count=$(kcadm get clients -r pegelhub \
  -q clientId=integration-preserved-client --fields id \
  | jq 'length')
[ "$marker_count" -eq 1 ] \
  || fail "Normal Keycloak recreation changed existing identity state."

printf '%s\n' "Disposable staging Keycloak bootstrap integration checks passed."
