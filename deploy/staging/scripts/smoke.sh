#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
COMPOSE_FILE="$DEPLOY_DIR/compose.yaml"
ENV_FILE="${PEGELHUB_STAGING_ENV_FILE:-$DEPLOY_DIR/.env}"

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
  COMPOSE_PROFILES="$compose_profiles" \
    docker compose \
      -p "$compose_project_name" \
      --env-file "$ENV_FILE" \
      -f "$COMPOSE_FILE" \
      "$@"
}

env_value() {
  key="$1"
  awk -F= -v key="$key" '
    $0 !~ /^[[:space:]]*(#|$)/ {
      if ($1 == key) {
        value = substr($0, length($1) + 2)
        gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
        gsub(/^"|"$/, "", value)
        gsub(/^'\''|'\''$/, "", value)
        print value
      }
    }
  ' "$ENV_FILE" | tail -n 1
}

api_hostname=$(env_value PEGELHUB_API_HOSTNAME)
frontend_hostname=$(env_value PEGELHUB_FRONTEND_HOSTNAME)
keycloak_hostname=$(env_value PEGELHUB_KEYCLOAK_HOSTNAME)
compose_project_name=$(env_value COMPOSE_PROJECT_NAME)
compose_profiles=$(env_value COMPOSE_PROFILES)

[ -n "$api_hostname" ] || fail "PEGELHUB_API_HOSTNAME is missing."
[ -n "$keycloak_hostname" ] || fail "PEGELHUB_KEYCLOAK_HOSTNAME is missing."
case "$compose_project_name" in
  pegelhub-staging|pegelhub-keycloak-test-*) ;;
  *) fail "COMPOSE_PROJECT_NAME must identify the staging or disposable Keycloak test project." ;;
esac

API_BASE_URL=${API_BASE_URL:-https://$api_hostname}
FRONTEND_BASE_URL=${FRONTEND_BASE_URL:-https://$frontend_hostname}
KEYCLOAK_ISSUER_URI=${KEYCLOAK_ISSUER_URI:-https://$keycloak_hostname/realms/pegelhub}

unset \
  COMPOSE_PROJECT_NAME \
  COMPOSE_PROFILES \
  FTP_CONFIG_DIR \
  PEGELHUB_FRONTEND_HOSTNAME \
  PEGELHUB_API_HOSTNAME \
  PEGELHUB_KEYCLOAK_HOSTNAME \
  PEGELHUB_FRONTEND_IMAGE \
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
  FLYWAY_BASELINE_ON_MIGRATE \
  FTP_JAVA_TOOL_OPTIONS

if printf '%s' "$compose_profiles" | grep -Eq '(^|.*,)[[:space:]]*frontend[[:space:]]*(,.*|$)'; then
  [ -n "$frontend_hostname" ] || fail "PEGELHUB_FRONTEND_HOSTNAME is missing."
  printf '%s\n' "Checking public frontend route..."
  retry "Public frontend route" sh -c 'curl -fsS "$1/" >/dev/null' sh "$FRONTEND_BASE_URL"
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

check_ftp_connector() {
  compose ps --status running ftp-connector | grep -q ftp-connector
}

printf '%s\n' "Checking internal Core actuator health..."
retry "Internal Core actuator health" check_core_health

printf '%s\n' "Checking internal Keycloak management health..."
retry "Internal Keycloak management health" check_keycloak_health

printf '%s\n' "Checking FTP connector is running..."
retry "FTP connector container" check_ftp_connector

printf '%s\n' "Staging smoke checks passed."
