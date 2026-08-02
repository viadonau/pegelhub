#!/bin/sh

# Initializes the PegelHub realm in a fresh or deliberately emptied staging
# Keycloak database. The script validates protected staging configuration,
# serializes against normal deploys, requires Keycloak to be stopped, runs the
# dedicated offline importer, and then starts the normal Keycloak service.
# Re-running it does not migrate or overwrite an existing realm, and it never
# resets the database or provisions users and service clients.
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
COMPOSE_FILE="$DEPLOY_DIR/compose.yaml"
BOOTSTRAP_COMPOSE_FILE="$DEPLOY_DIR/keycloak-bootstrap.compose.yaml"
ENV_FILE="${PEGELHUB_STAGING_ENV_FILE:-$DEPLOY_DIR/.env}"
STATE_DIR="$DEPLOY_DIR/state"
LOCK_DIR="$STATE_DIR/keycloak-bootstrap.lock"
LOCK_OWNED=false
LOCK_TOKEN=""

fail() {
  printf '%s\n' "ERROR: $*" >&2
  exit 1
}

compose() {
  COMPOSE_PROJECT_NAME="$compose_project_name" \
  COMPOSE_PROFILES=keycloak-bootstrap \
  PEGELHUB_FRONTEND_HOSTNAME="$frontend_hostname" \
  PEGELHUB_KEYCLOAK_HOSTNAME="$keycloak_hostname" \
  KEYCLOAK_ADMIN_USER="$keycloak_admin_user" \
  KEYCLOAK_ADMIN_PASSWORD="$keycloak_admin_password" \
  KEYCLOAK_DB="$keycloak_db" \
  KEYCLOAK_DB_PASSWORD="$keycloak_db_password" \
    docker compose \
      -p "$compose_project_name" \
      --env-file "$ENV_FILE" \
      -f "$COMPOSE_FILE" \
      -f "$BOOTSTRAP_COMPOSE_FILE" \
      --profile keycloak-bootstrap \
      "$@"
}

env_value() {
  key="$1"
  [ -f "$ENV_FILE" ] || return 0
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

reject_placeholder() {
  variable_name="$1"
  value=$(env_value "$variable_name")
  case "$value" in
    ""|replace-with-staging-*|CHANGE_ME|changeme)
      fail "$variable_name must be initialized in the protected staging env file."
      ;;
  esac
}

validate_public_hostname() {
  variable_name="$1"
  hostname=$(env_value "$variable_name")
  normalized=$(printf '%s' "$hostname" | tr '[:upper:]' '[:lower:]')

  case "$normalized" in
    test|*.test)
      case "$compose_project_name" in
        pegelhub-keycloak-test-*) ;;
        *) fail "$variable_name must not use a reserved test hostname for staging." ;;
      esac
      ;;
  esac
  case "$normalized" in
    ""|localhost|*.localhost|example|*.example|example.com|*.example.com|invalid|*.invalid)
      fail "$variable_name must be a real staging hostname, not a placeholder or loopback address."
      ;;
    *://*|*/*|*:*|.*|*.|*..*|*[!a-z0-9.-]*)
      fail "$variable_name must contain only a public DNS hostname without a scheme, port, or path."
      ;;
    *.*) ;;
    *)
      fail "$variable_name must be a fully qualified staging hostname."
      ;;
  esac
  if printf '%s\n' "$normalized" | grep -Eq '^[0-9]+([.][0-9]+){3}$|(^|[.])-|-([.]|$)'; then
    fail "$variable_name must be a DNS hostname, not an IP address or malformed label."
  fi
}

cleanup() {
  if [ "$LOCK_OWNED" = "true" ]; then
    LOCK_OWNED=false
    recorded_token=""
    if [ -f "$LOCK_DIR/owner" ]; then
      IFS= read -r recorded_token < "$LOCK_DIR/owner" || true
    fi
    if [ -n "$LOCK_TOKEN" ] && [ "$recorded_token" = "$LOCK_TOKEN" ]; then
      rm -f "$LOCK_DIR/owner"
      rmdir "$LOCK_DIR" >/dev/null 2>&1 || true
    fi
  fi
}

exit_on_signal() {
  exit_status="$1"
  trap - HUP INT TERM
  cleanup
  exit "$exit_status"
}

acquire_lock() {
  mkdir -p "$STATE_DIR"
  if ! mkdir "$LOCK_DIR" 2>/dev/null; then
    fail "Another staging deploy or Keycloak bootstrap operation is active."
  fi
  chmod 700 "$LOCK_DIR"
  LOCK_TOKEN="bootstrap-$$-$(date +%s)"
  printf '%s\n' "$LOCK_TOKEN" > "$LOCK_DIR/owner"
  chmod 600 "$LOCK_DIR/owner"
  LOCK_OWNED=true
  trap cleanup EXIT
  trap 'exit_on_signal 129' HUP
  trap 'exit_on_signal 130' INT
  trap 'exit_on_signal 143' TERM
}

reject_active_keycloak() {
  container_ids=$(compose ps --all -q keycloak)
  for container_id in $container_ids; do
    state=$(docker inspect --format \
      '{{.State.Running}} {{.State.Paused}} {{.State.Restarting}}' \
      "$container_id")
    [ "$state" = "false false false" ] \
      || fail "Stop staging Keycloak completely before offline realm bootstrap. The script never stops it automatically."
  done
}

[ -f "$ENV_FILE" ] || fail "Missing protected staging env file: $ENV_FILE"
[ "$(env_value PEGELHUB_ENVIRONMENT)" = "staging" ] \
  || fail "PEGELHUB_ENVIRONMENT must be staging."
[ "$(env_value PEGELHUB_DEPLOY_MARKER)" = "pegelhub-staging" ] \
  || fail "PEGELHUB_DEPLOY_MARKER must be pegelhub-staging."
compose_project_name=$(env_value COMPOSE_PROJECT_NAME)
case "$compose_project_name" in
  pegelhub-staging|pegelhub-keycloak-test-*) ;;
  *) fail "COMPOSE_PROJECT_NAME must identify the staging or disposable Keycloak test project." ;;
esac
validate_public_hostname PEGELHUB_FRONTEND_HOSTNAME
validate_public_hostname PEGELHUB_KEYCLOAK_HOSTNAME
frontend_hostname=$(env_value PEGELHUB_FRONTEND_HOSTNAME)
keycloak_hostname=$(env_value PEGELHUB_KEYCLOAK_HOSTNAME)
keycloak_admin_user=$(env_value KEYCLOAK_ADMIN_USER)
keycloak_admin_password=$(env_value KEYCLOAK_ADMIN_PASSWORD)
keycloak_db=$(env_value KEYCLOAK_DB)
keycloak_db_password=$(env_value KEYCLOAK_DB_PASSWORD)
[ -n "$keycloak_admin_user" ] \
  || fail "KEYCLOAK_ADMIN_USER is required."
[ -n "$keycloak_db" ] || fail "KEYCLOAK_DB is required."
reject_placeholder KEYCLOAK_DB_PASSWORD
reject_placeholder KEYCLOAK_ADMIN_PASSWORD
acquire_lock
reject_active_keycloak

printf '%s\n' "Starting the staging Keycloak database only..."
compose up -d --wait keycloak-db

printf '%s\n' "Importing the staging realm only when it is absent..."
compose run --rm --no-deps -T keycloak-realm-bootstrap

printf '%s\n' "Starting staging Keycloak without startup import..."
compose up -d --wait keycloak

printf '%s\n' "Staging Keycloak bootstrap complete."
