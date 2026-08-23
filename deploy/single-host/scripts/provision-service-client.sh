#!/bin/sh

# Idempotently provisions one confidential Keycloak service client. Existing
# credentials are compared with the protected host file and are never rotated.
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
STATE_DIR=${PEGELHUB_STATE_DIR:-}
[ -n "$STATE_DIR" ] || { printf 'ERROR: Set PEGELHUB_STATE_DIR.\n' >&2; exit 1; }
LOCK_DIR="$STATE_DIR/operation.lock"
LOCK_OWNED=false
KCADM_CONFIG="/tmp/pegelhub-service-client-$$.config"
KCADM_CONFIG_CREATED=false

usage() {
  cat <<USAGE
Usage: $0 <client-id> <secret-file> <core-role> [<core-role> ...]

The secret file is created with mode 600 when the client is new. For an
existing client it must already exist and contain the unchanged credential.
USAGE
}

fail() {
  printf '%s\n' "ERROR: $*" >&2
  exit 1
}

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

cleanup() {
  if [ "$KCADM_CONFIG_CREATED" = "true" ]; then
    compose exec -T keycloak rm -f "$KCADM_CONFIG" >/dev/null 2>&1 || true
    KCADM_CONFIG_CREATED=false
  fi
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

acquire_operation_lock() {
  mkdir -p "$STATE_DIR"
  if ! mkdir "$LOCK_DIR" 2>/dev/null; then
    fail "Another deploy, bootstrap, or provisioning operation is active."
  fi
  chmod 700 "$LOCK_DIR"
  LOCK_OWNED=true
}

kcadm() {
  compose exec -T keycloak \
    /opt/keycloak/bin/kcadm.sh "$@" --config "$KCADM_CONFIG"
}

single_id() {
  resource="$1"
  query="$2"
  result=$(kcadm get "$resource" -r pegelhub -q "$query" \
    --fields id --format csv --noquotes | tr -d '\r')
  [ -n "$result" ] || fail "Missing Keycloak object for $query."
  [ "$(printf '%s\n' "$result" | wc -l | tr -d ' ')" -eq 1 ] \
    || fail "Expected exactly one Keycloak object for $query."
  printf '%s\n' "$result"
}

validate_role() {
  case "$1" in
    measurement:read|measurement:write|metadata:read|metadata:write|system:admin|telemetry:read|telemetry:write) ;;
    *) fail "Unsupported PegelHub Core role: $1" ;;
  esac
}

create_secret_file() {
  secret_parent=$(dirname -- "$secret_file")
  mkdir -p "$secret_parent"
  chmod 700 "$secret_parent"
  secret_temp="$secret_file.tmp.$$"
  openssl rand -hex 32 | tr -d '\r\n' > "$secret_temp"
  chmod 600 "$secret_temp"
  mv "$secret_temp" "$secret_file"
}

configure_kcadm() {
  compose exec -T keycloak sh -eu -c '
    export KC_CLI_PASSWORD="$KC_BOOTSTRAP_ADMIN_PASSWORD"
    /opt/keycloak/bin/kcadm.sh config credentials \
      --config "$1" \
      --server http://localhost:8080 \
      --realm master \
      --user "$KC_BOOTSTRAP_ADMIN_USERNAME" >/dev/null 2>&1
    unset KC_CLI_PASSWORD KC_BOOTSTRAP_ADMIN_PASSWORD
  ' sh "$KCADM_CONFIG"
  KCADM_CONFIG_CREATED=true
}

create_client() {
  printf '%s\n' "$client_secret" | compose exec -T keycloak sh -eu -c '
    IFS= read -r client_secret
    /opt/keycloak/bin/kcadm.sh create clients -r pegelhub \
      --config "$1" \
      -s "clientId=$2" \
      -s enabled=true \
      -s publicClient=false \
      -s bearerOnly=false \
      -s serviceAccountsEnabled=true \
      -s standardFlowEnabled=false \
      -s implicitFlowEnabled=false \
      -s directAccessGrantsEnabled=false \
      -s fullScopeAllowed=false \
      -s "secret=$client_secret" >/dev/null 2>&1
    unset client_secret
  ' sh "$KCADM_CONFIG" "$client_id"
}

reconcile_client_settings() {
  kcadm update "clients/$client_uuid" -r pegelhub \
    -s enabled=true \
    -s publicClient=false \
    -s bearerOnly=false \
    -s serviceAccountsEnabled=true \
    -s standardFlowEnabled=false \
    -s implicitFlowEnabled=false \
    -s directAccessGrantsEnabled=false \
    -s fullScopeAllowed=false >/dev/null
}

reconcile_default_scopes() {
  current_scopes=$(kcadm get "clients/$client_uuid/default-client-scopes" -r pegelhub)
  printf '%s\n' "$current_scopes" | jq -r '.[] | [.id, .name] | @tsv' \
    | while IFS="$(printf '\t')" read -r scope_id scope_name; do
        case " basic pegelhub-core-roles pegelhub-core-audience pegelhub-client-actor " in
          *" $scope_name "*) ;;
          *) kcadm delete "clients/$client_uuid/default-client-scopes/$scope_id" -r pegelhub >/dev/null ;;
        esac
      done

  for scope_name in basic pegelhub-core-roles pegelhub-core-audience pegelhub-client-actor; do
    scope_id=$(kcadm get client-scopes -r pegelhub \
      | jq -er --arg name "$scope_name" '
          map(select(.name == $name))
          | if length == 1 then .[0].id else error("scope count") end
        ') || fail "Expected exactly one Keycloak client scope named $scope_name."
    if ! printf '%s\n' "$current_scopes" | jq -e --arg id "$scope_id" \
      'any(.[]; .id == $id)' >/dev/null; then
      kcadm update "clients/$client_uuid/default-client-scopes/$scope_id" -r pegelhub >/dev/null
    fi
  done
}

reconcile_core_roles() {
  core_client_uuid=$(single_id clients clientId=pegelhub-core-api)
  service_account_uuid=$(kcadm get "clients/$client_uuid/service-account-user" -r pegelhub \
    | jq -er '.id')
  role_mapping_path="users/$service_account_uuid/role-mappings/clients/$core_client_uuid"
  scope_mapping_path="clients/$client_uuid/scope-mappings/clients/$core_client_uuid"
  current_roles=$(kcadm get "$role_mapping_path" -r pegelhub)
  if [ "$(printf '%s\n' "$current_roles" | jq 'length')" -gt 0 ]; then
    kcadm delete "$role_mapping_path" -r pegelhub -b "$current_roles" >/dev/null
  fi
  current_scope_roles=$(kcadm get "$scope_mapping_path" -r pegelhub)
  if [ "$(printf '%s\n' "$current_scope_roles" | jq 'length')" -gt 0 ]; then
    kcadm delete "$scope_mapping_path" -r pegelhub -b "$current_scope_roles" >/dev/null
  fi

  for role in $requested_roles; do
    role_json=$(kcadm get "clients/$core_client_uuid/roles/$role" -r pegelhub)
    kcadm create "$role_mapping_path" -r pegelhub -b "[$role_json]" >/dev/null
    kcadm create "$scope_mapping_path" -r pegelhub -b "[$role_json]" >/dev/null
  done

  actual_roles=$(kcadm get "$role_mapping_path" -r pegelhub \
    | jq -r '.[].name' | sort | tr '\n' ' ' | sed 's/ $//')
  expected_roles=$(printf '%s\n' $requested_roles | sort | tr '\n' ' ' | sed 's/ $//')
  [ "$actual_roles" = "$expected_roles" ] \
    || fail "Keycloak did not apply the exact requested Core roles."
  actual_scope_roles=$(kcadm get "$scope_mapping_path" -r pegelhub \
    | jq -r '.[].name' | sort | tr '\n' ' ' | sed 's/ $//')
  [ "$actual_scope_roles" = "$expected_roles" ] \
    || fail "Keycloak did not apply the exact requested Core scope mappings."
}

[ "$#" -ge 3 ] || { usage >&2; exit 2; }
client_id="$1"
secret_file="$2"
shift 2

case "$client_id" in
  ""|*[!A-Za-z0-9._-]*) fail "Client ID may contain only letters, digits, dots, underscores, and hyphens." ;;
  pegelhub-core-api|pegelhub-frontend|pegelhub-cutover-*) fail "Reserved Keycloak client ID: $client_id" ;;
esac
case "$secret_file" in
  /*) ;;
  *) fail "Secret-file destination must be an absolute path on the target host." ;;
esac
[ ! -L "$secret_file" ] || fail "Secret-file destination must not be a symlink."
if [ -e "$secret_file" ] && [ ! -f "$secret_file" ]; then
  fail "Secret-file destination must be a regular file."
fi

requested_roles=""
seen_roles=" "
for role in "$@"; do
  validate_role "$role"
  case "$seen_roles" in
    *" $role "*) fail "Core role was requested more than once: $role" ;;
  esac
  seen_roles="$seen_roles$role "
  requested_roles="${requested_roles}${requested_roles:+ }$role"
done

command -v docker >/dev/null 2>&1 || fail "docker is required."
command -v jq >/dev/null 2>&1 || fail "jq is required."
command -v openssl >/dev/null 2>&1 || fail "openssl is required."
[ -f "$ENV_FILE" ] || fail "Missing protected deployment env file: $ENV_FILE"
compose_project_name=$(env_value COMPOSE_PROJECT_NAME)
[ -n "$compose_project_name" ] || fail "COMPOSE_PROJECT_NAME is required."

keycloak_container=$(compose ps -q keycloak)
[ -n "$keycloak_container" ] || fail "The target Keycloak service is not running."
[ "$(docker inspect --format '{{.State.Running}}' "$keycloak_container")" = "true" ] \
  || fail "The target Keycloak service is not running."

umask 077
acquire_operation_lock
trap cleanup EXIT
trap 'exit_on_signal 129' HUP
trap 'exit_on_signal 130' INT
trap 'exit_on_signal 143' TERM
configure_kcadm

client_matches=$(kcadm get clients -r pegelhub -q "clientId=$client_id" --fields id \
  | jq 'length')
case "$client_matches" in
  0)
    [ -f "$secret_file" ] || create_secret_file
    chmod 600 "$secret_file"
    client_secret=$(cat "$secret_file")
    [ -n "$client_secret" ] || fail "The protected client secret file is empty."
    create_client
    ;;
  1)
    [ -f "$secret_file" ] \
      || fail "Existing client credentials are not rotated; restore its protected secret file."
    chmod 600 "$secret_file"
    client_secret=$(cat "$secret_file")
    client_uuid=$(single_id clients "clientId=$client_id")
    keycloak_secret=$(kcadm get "clients/$client_uuid/client-secret" -r pegelhub | jq -er '.value')
    [ "$client_secret" = "$keycloak_secret" ] \
      || fail "The protected secret file does not match the existing client; refusing rotation."
    unset keycloak_secret
    ;;
  *) fail "More than one Keycloak client has clientId=$client_id." ;;
esac
unset client_secret

client_uuid=$(single_id clients "clientId=$client_id")
reconcile_client_settings
reconcile_default_scopes
reconcile_core_roles

printf '%s\n' "Provisioned service client $client_id with the exact requested Core roles."
