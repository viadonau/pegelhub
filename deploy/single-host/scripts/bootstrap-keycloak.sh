#!/bin/sh

# Initializes the PegelHub realm in a fresh or deliberately emptied single-host
# Keycloak database. The script validates protected host configuration,
# serializes against normal deploys, requires Keycloak to be stopped, runs the
# dedicated offline importer, and then starts the normal Keycloak service.
# Re-running it does not migrate or overwrite an existing realm, and it never
# resets the database or provisions users and service clients.
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
. "$SCRIPT_DIR/../../lib/env-file.sh"
COMPOSE_FILE="$DEPLOY_DIR/compose.yaml"
BOOTSTRAP_COMPOSE_FILE="$DEPLOY_DIR/keycloak-bootstrap.compose.yaml"
CONFIG_DIR=${PEGELHUB_CONFIG_DIR:-}
ENV_FILE=${PEGELHUB_ENV_FILE:-}
if [ -z "$ENV_FILE" ]; then
  [ -n "$CONFIG_DIR" ] || { printf 'ERROR: Set PEGELHUB_CONFIG_DIR or PEGELHUB_ENV_FILE.\n' >&2; exit 1; }
  ENV_FILE="$CONFIG_DIR/pegelhub.env"
fi
[ -n "$CONFIG_DIR" ] || CONFIG_DIR=$(CDPATH= cd -- "$(dirname -- "$ENV_FILE")" && pwd)
STATE_DIR="${PEGELHUB_STATE_DIR:-}"
[ -n "$STATE_DIR" ] || { printf 'ERROR: Set PEGELHUB_STATE_DIR.\n' >&2; exit 1; }
LOCK_DIR="$STATE_DIR/operation.lock"
LOCK_OWNED=false

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

reject_placeholder() {
  variable_name="$1"
  value=$(env_value "$variable_name")
  case "$value" in
    ""|replace-with-staging-*|replace-with-pegelhub-*|CHANGE_ME|changeme)
      fail "$variable_name must be initialized in the protected deployment env file."
      ;;
  esac
}

validate_deployment_hostname() {
  variable_name="$1"
  hostname=$(env_value "$variable_name")
  normalized=$(printf '%s' "$hostname" | tr '[:upper:]' '[:lower:]')

  case "$normalized" in
    test|*.test)
      case "$compose_project_name" in
        pegelhub-keycloak-test-*) ;;
        *) fail "$variable_name must not use a reserved test hostname." ;;
      esac
      ;;
  esac
  case "$normalized" in
    ""|localhost|*.localhost|example|*.example|example.com|*.example.com|invalid|*.invalid)
      fail "$variable_name must be a real hostname, not a placeholder or loopback address."
      ;;
    *://*|*/*|*:*|.*|*.|*..*|*[!a-z0-9.-]*)
      fail "$variable_name must contain only a fully qualified DNS hostname without a scheme, port, or path."
      ;;
    *.*) ;;
    *)
      fail "$variable_name must be a fully qualified hostname."
      ;;
  esac
  if printf '%s\n' "$normalized" | grep -Eq '^[0-9]+([.][0-9]+){3}$|(^|[.])-|-([.]|$)'; then
    fail "$variable_name must be a DNS hostname, not an IP address or malformed label."
  fi
}

cleanup() {
  if [ "$LOCK_OWNED" = "true" ]; then
    LOCK_OWNED=false
    rmdir "$LOCK_DIR" >/dev/null 2>&1 || true
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
    fail "Another deploy or Keycloak bootstrap operation is active."
  fi
  chmod 700 "$LOCK_DIR"
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
      || fail "Stop Keycloak completely before offline realm bootstrap. The script never stops it automatically."
  done
}

[ -f "$ENV_FILE" ] || fail "Missing protected deployment env file: $ENV_FILE"
compose_project_name=$(env_value COMPOSE_PROJECT_NAME)
[ -n "$compose_project_name" ] || fail "COMPOSE_PROJECT_NAME is required."
validate_deployment_hostname PEGELHUB_FRONTEND_HOSTNAME
validate_deployment_hostname PEGELHUB_KEYCLOAK_HOSTNAME
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

printf '%s\n' "Starting the Keycloak database only..."
compose up -d --wait keycloak-db

printf '%s\n' "Importing the realm only when it is absent..."
compose run --rm --no-deps -T keycloak-realm-bootstrap

printf '%s\n' "Starting Keycloak without startup import..."
compose up -d --wait keycloak

printf '%s\n' "Keycloak bootstrap complete."
