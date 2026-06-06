#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
COMPOSE_FILE="$DEPLOY_DIR/compose.yaml"
ENV_FILE="$DEPLOY_DIR/.env"

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
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"
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

[ -n "$api_hostname" ] || fail "PEGELHUB_API_HOSTNAME is missing."
[ -n "$keycloak_hostname" ] || fail "PEGELHUB_KEYCLOAK_HOSTNAME is missing."

API_BASE_URL=${API_BASE_URL:-https://$api_hostname}
FRONTEND_BASE_URL=${FRONTEND_BASE_URL:-https://$frontend_hostname}
KEYCLOAK_ISSUER_URI=${KEYCLOAK_ISSUER_URI:-https://$keycloak_hostname/realms/pegelhub}

compose_profiles=$(env_value COMPOSE_PROFILES)
if printf '%s' "$compose_profiles" | grep -Eq '(^|.*,)[[:space:]]*frontend[[:space:]]*(,.*|$)'; then
  [ -n "$frontend_hostname" ] || fail "PEGELHUB_FRONTEND_HOSTNAME is missing."
  printf '%s\n' "Checking public frontend route..."
  retry "Public frontend route" sh -c 'curl -fsS "$1/" >/dev/null' sh "$FRONTEND_BASE_URL"
fi

printf '%s\n' "Checking public API route..."
retry "Public API route" sh -c 'curl -fsS "$1/api/v1/measurement/systemTime" >/dev/null' sh "$API_BASE_URL"

printf '%s\n' "Checking public Keycloak issuer discovery..."
retry "Public Keycloak issuer discovery" sh -c 'curl -fsS "$1/.well-known/openid-configuration" | grep -q "\"issuer\""' sh "$KEYCLOAK_ISSUER_URI"

printf '%s\n' "Checking internal Core actuator health..."
retry "Internal Core actuator health" sh -c 'docker compose --env-file "$1" -f "$2" exec -T caddy wget -qO- http://core-app:8081/actuator/health | grep -q "\"status\":\"UP\""' sh "$ENV_FILE" "$COMPOSE_FILE"

printf '%s\n' "Checking internal Keycloak management health..."
retry "Internal Keycloak management health" sh -c 'docker compose --env-file "$1" -f "$2" exec -T caddy wget -qO- http://keycloak:9000/health/ready | grep -q "\"status\""' sh "$ENV_FILE" "$COMPOSE_FILE"

printf '%s\n' "Checking FTP connector is running..."
retry "FTP connector container" sh -c 'docker compose --env-file "$1" -f "$2" ps --status running ftp-connector | grep -q ftp-connector' sh "$ENV_FILE" "$COMPOSE_FILE"

printf '%s\n' "Staging smoke checks passed."
