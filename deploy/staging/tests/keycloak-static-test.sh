#!/bin/sh

# Fast policy checks for the staging realm seed and bootstrap wiring. This test
# inspects JSON, shell syntax, rendered Compose configuration, and safety guards
# without starting Keycloak containers. It rejects committed identity data,
# secrets, local origins, and accidental exposure of the importer to routine
# deploys.
set -eu

TEST_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$TEST_DIR/.." && pwd)
REPO_DIR=$(CDPATH= cd -- "$DEPLOY_DIR/../.." && pwd)
REALM_FILE="$DEPLOY_DIR/keycloak/pegelhub-realm.json"
LOCAL_REALM_FILE="$REPO_DIR/core/docker/keycloak/import/pegelhub-realm.json"
COMPOSE_FILE="$DEPLOY_DIR/compose.yaml"
BOOTSTRAP_COMPOSE_FILE="$DEPLOY_DIR/keycloak-bootstrap.compose.yaml"
ENV_FILE="$DEPLOY_DIR/.env.example"

fail() {
  printf '%s\n' "ERROR: $*" >&2
  exit 1
}

command -v jq >/dev/null 2>&1 || fail "jq is required."
command -v docker >/dev/null 2>&1 || fail "docker is required."

jq empty "$REALM_FILE"
jq empty "$LOCAL_REALM_FILE"

jq -e '
  .realm == "pegelhub"
  and .enabled == true
  and .displayName == "PegelHub Staging"
  and .loginTheme == "pegelhub"
  and .internationalizationEnabled == true
  and .supportedLocales == ["de"]
  and .defaultLocale == "de"
  and .users == []
  and ([.clients[].clientId] | sort)
    == ["pegelhub-core-api", "pegelhub-frontend"]
  and ([.roles.client["pegelhub-core-api"][].name] | sort)
    == [
      "measurement:read",
      "measurement:write",
      "metadata:read",
      "metadata:write",
      "system:admin",
      "telemetry:read",
      "telemetry:write"
    ]
  and ([.clientScopes[].name] | sort)
    == [
      "basic",
      "pegelhub-client-actor",
      "pegelhub-core-audience",
      "pegelhub-core-roles",
      "pegelhub-user-actor",
      "profile"
    ]
  and (
    .clientScopes[]
    | select(.name == "pegelhub-core-audience")
    | .protocolMappers[]
    | select(.protocolMapper == "oidc-audience-mapper")
    | .config["included.client.audience"] == "pegelhub-core-api"
  )
  and (
    .clients[]
    | select(.clientId == "pegelhub-frontend")
    | .publicClient == true
      and .standardFlowEnabled == true
      and .fullScopeAllowed == false
      and .rootUrl == "${PEGELHUB_FRONTEND_URL}"
      and .redirectUris == ["${PEGELHUB_FRONTEND_URL}/*"]
      and .webOrigins == ["${PEGELHUB_FRONTEND_URL}"]
      and .attributes["pkce.code.challenge.method"] == "S256"
  )
  and .clientScopeMappings["pegelhub-core-api"] == [
    {
      "client": "pegelhub-frontend",
      "roles": ["metadata:read", "measurement:read"]
    }
  ]
  and ([.. | objects | select(has("secret") or has("credentials"))] | length) == 0
  and (tostring | test("localhost|127[.]0[.]0[.]1|local-"; "i") | not)
' "$REALM_FILE" >/dev/null || fail "Staging realm policy check failed."

jq -e --slurp '
  .[0] as $staging
  | .[1] as $local
  | $staging.roles.client["pegelhub-core-api"]
      == $local.roles.client["pegelhub-core-api"]
    and (
      [$staging.clientScopes[] | select(.name | startswith("pegelhub-"))]
      == [$local.clientScopes[] | select(.name | startswith("pegelhub-"))]
    )
' "$REALM_FILE" "$LOCAL_REALM_FILE" >/dev/null \
  || fail "Staging and local API authorization contracts have drifted."

for script in "$DEPLOY_DIR"/scripts/*.sh "$DEPLOY_DIR"/tests/*.sh; do
  sh -n "$script"
done

if grep -Eq '(^|[[:space:]])set[[:space:]]+-[^[:space:]]*x' \
  "$DEPLOY_DIR/scripts/bootstrap-keycloak.sh" \
  "$DEPLOY_DIR/scripts/deploy.sh"; then
  fail "Staging operation scripts must never enable shell tracing."
fi

if grep -F '${FTP_CONFIG_DIR:-./ftp-config}:/app/config:ro' \
  "$COMPOSE_FILE" >/dev/null; then
  fail "FTP config must use long volume syntax for Compose 2.x compatibility."
fi

compose_json=$(mktemp "${TMPDIR:-/tmp}/pegelhub-keycloak-compose.XXXXXX")
bootstrap_compose_json=$(mktemp "${TMPDIR:-/tmp}/pegelhub-keycloak-bootstrap-compose.XXXXXX")
invalid_keycloak_env=$(mktemp "${TMPDIR:-/tmp}/pegelhub-keycloak-invalid-env.XXXXXX")
invalid_keycloak_log=$(mktemp "${TMPDIR:-/tmp}/pegelhub-keycloak-invalid-log.XXXXXX")
cleanup() {
  rm -f \
    "$compose_json" \
    "$bootstrap_compose_json" \
    "$invalid_keycloak_env" \
    "$invalid_keycloak_log"
}
trap cleanup EXIT
trap 'exit 129' HUP
trap 'exit 130' INT
trap 'exit 143' TERM

if PEGELHUB_STAGING_ENV_FILE="$ENV_FILE" \
  "$DEPLOY_DIR/scripts/bootstrap-keycloak.sh" >/dev/null 2>&1; then
  fail "Keycloak bootstrap accepted committed example hostnames."
fi

sed \
  -e 's/^COMPOSE_PROJECT_NAME=.*/COMPOSE_PROJECT_NAME=pegelhub-keycloak-test-static/' \
  -e 's/^PEGELHUB_FRONTEND_HOSTNAME=.*/PEGELHUB_FRONTEND_HOSTNAME=frontend.keycloak.test/' \
  "$ENV_FILE" > "$invalid_keycloak_env"
if PEGELHUB_STAGING_ENV_FILE="$invalid_keycloak_env" \
  "$DEPLOY_DIR/scripts/bootstrap-keycloak.sh" > "$invalid_keycloak_log" 2>&1; then
  fail "Keycloak bootstrap accepted a placeholder Keycloak hostname."
fi
grep -F 'PEGELHUB_KEYCLOAK_HOSTNAME must be a real staging hostname' \
  "$invalid_keycloak_log" >/dev/null \
  || fail "Keycloak bootstrap did not validate the protected Keycloak hostname."

if PEGELHUB_STAGING_ENV_FILE="$ENV_FILE" \
  "$DEPLOY_DIR/scripts/deploy.sh" --check sha-static-policy >/dev/null 2>&1; then
  fail "Routine staging validation accepted committed example hostnames."
fi

docker compose \
  --env-file "$ENV_FILE" \
  -f "$COMPOSE_FILE" \
  config --format json > "$compose_json"
docker compose \
  --env-file "$ENV_FILE" \
  -f "$COMPOSE_FILE" \
  -f "$BOOTSTRAP_COMPOSE_FILE" \
  --profile keycloak-bootstrap \
  config --format json > "$bootstrap_compose_json"

jq -e '
  .services.keycloak.command == ["start"]
  and (
    [.services.keycloak.volumes[]? | .target]
    | index("/opt/keycloak/data/import")
    | not
  )
  and .services.keycloak.healthcheck != null
  and (.services | has("keycloak-realm-bootstrap") | not)
  and (
    [
      .services["ftp-connector"].volumes[]
      | select(.target == "/app/config")
      | {type, target, read_only, expected_source: (.source | endswith("/deploy/staging/ftp-config"))}
    ] == [{"type":"bind","target":"/app/config","read_only":true,"expected_source":true}]
  )
' "$compose_json" >/dev/null \
  || fail "Routine staging Compose lifecycle policy check failed."

jq -e '
  .services["keycloak-realm-bootstrap"].command == [
    "import",
    "--file",
    "/opt/pegelhub/keycloak/pegelhub-realm.json",
    "--override",
    "false"
  ]
  and .services["keycloak-realm-bootstrap"].environment.PEGELHUB_FRONTEND_URL
    == "https://pegelhub-staging.example.com"
  and (.services | keys) == [
    "caddy",
    "core-app",
    "data-db",
    "ftp-connector",
    "influx-bucket-setup",
    "keycloak",
    "keycloak-db",
    "keycloak-realm-bootstrap",
    "meta-db"
  ]
  and (.services["keycloak-realm-bootstrap"].environment | keys) == [
    "KC_BOOTSTRAP_ADMIN_PASSWORD",
    "KC_BOOTSTRAP_ADMIN_USERNAME",
    "KC_DB",
    "KC_DB_PASSWORD",
    "KC_DB_URL",
    "KC_DB_USERNAME",
    "PEGELHUB_FRONTEND_URL"
  ]
' "$bootstrap_compose_json" >/dev/null \
  || fail "Explicit staging Keycloak bootstrap Compose policy check failed."

grep -F 'docker inspect --format' \
  "$DEPLOY_DIR/scripts/bootstrap-keycloak.sh" >/dev/null \
  || fail "Offline bootstrap must inspect all Keycloak container states."
grep -F 'keycloak-bootstrap.lock' \
  "$DEPLOY_DIR/scripts/bootstrap-keycloak.sh" \
  "$DEPLOY_DIR/scripts/deploy.sh" >/dev/null \
  || fail "Deploy and bootstrap must share a host operation lock."
grep -F 'acquire_operation_lock' \
  "$DEPLOY_DIR/scripts/deploy.sh" >/dev/null \
  || fail "Routine deploy must acquire the shared staging operation lock."
grep -F 'META_PASSWORD="$compose_meta_password"' \
  "$DEPLOY_DIR/scripts/deploy.sh" >/dev/null \
  || fail "Routine deploy must pin protected Compose values from the validated env file."
grep -F 'rm -f "$LEGACY_RENDERED_FILE"' \
  "$DEPLOY_DIR/scripts/deploy.sh" >/dev/null \
  || fail "Routine deploy must remove the legacy rendered secret artifact."

for signal_safe_script in \
  "$DEPLOY_DIR/scripts/bootstrap-keycloak.sh" \
  "$DEPLOY_DIR/scripts/deploy.sh"; do
  grep -F "trap 'exit_on_signal 143' TERM" "$signal_safe_script" >/dev/null \
    || fail "Staging operation scripts must exit after a termination signal."
done

git -C "$REPO_DIR" check-ignore -q deploy/staging/ftp-config/connector.yaml \
  || fail "The server-local FTP config path must remain ignored."

printf '%s\n' "Staging Keycloak static checks passed."
