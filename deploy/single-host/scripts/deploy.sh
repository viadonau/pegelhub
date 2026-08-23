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
STATE_DIR="${PEGELHUB_STATE_DIR:-}"
[ -n "$STATE_DIR" ] || { printf 'ERROR: Set PEGELHUB_STATE_DIR.\n' >&2; exit 1; }
CURRENT_RELEASE_FILE="$STATE_DIR/current-release.env"
LEGACY_RENDERED_FILE="$STATE_DIR/compose.rendered.yaml"
LOCK_DIR="$STATE_DIR/operation.lock"

CHECK_ONLY=false
ROLLBACK=false
REFRESH_KEYCLOAK=false
RESET_DATA_CONFIRMATION=""
REQUESTED_TAG=""
LOCK_OWNED=false
compose_structure=""

usage() {
  cat <<USAGE
Usage:
  $0 [--check] [--refresh-keycloak] <image-tag>
  $0 --reset-data <compose-project-name> <image-tag>
  $0 --rollback

Examples:
  $0 --check sha-42bd19b
  $0 --refresh-keycloak sha-42bd19b
  $0 --reset-data pegelhub-staging sha-42bd19b
  $0 sha-42bd19b
  $0 v0.1.0
  $0 --rollback
USAGE
}

fail() {
  printf '%s\n' "ERROR: $*" >&2
  exit 1
}

cleanup() {
  [ -z "$compose_structure" ] || rm -f "$compose_structure"
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
    fail "Another deploy or Keycloak bootstrap operation is active."
  fi
  chmod 700 "$LOCK_DIR"
  LOCK_OWNED=true
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --check)
      CHECK_ONLY=true
      ;;
    --rollback)
      ROLLBACK=true
      ;;
    --refresh-keycloak)
      REFRESH_KEYCLOAK=true
      ;;
    --reset-data)
      [ "$#" -ge 2 ] || fail "--reset-data requires the Compose project name as confirmation."
      RESET_DATA_CONFIRMATION="$2"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    -*)
      fail "Unknown option: $1"
      ;;
    *)
      [ -z "$REQUESTED_TAG" ] || fail "Only one image tag may be provided."
      REQUESTED_TAG="$1"
      ;;
  esac
  shift
done

compose() {
  COMPOSE_PROJECT_NAME="$compose_project_name" \
  COMPOSE_IGNORE_ORPHANS=true \
  COMPOSE_REMOVE_ORPHANS=false \
  PEGELHUB_FRONTEND_HOSTNAME="$compose_frontend_hostname" \
  PEGELHUB_API_HOSTNAME="$compose_api_hostname" \
  PEGELHUB_KEYCLOAK_HOSTNAME="$compose_keycloak_hostname" \
  PEGELHUB_HTTP_BIND="$compose_http_bind" \
  PEGELHUB_HTTPS_BIND="$compose_https_bind" \
  PEGELHUB_HTTPS_URL_SUFFIX="$compose_https_url_suffix" \
  PEGELHUB_HTTPS_CONTAINER_PORT="$compose_https_container_port" \
  PEGELHUB_TLS_MODE="$compose_tls_mode" \
  PEGELHUB_TRUST_MODE="$compose_trust_mode" \
  PEGELHUB_TLS_SERVER_DIR="$compose_tls_server_dir" \
  PEGELHUB_TRUST_DIR="$compose_trust_dir" \
  META_PASSWORD="$compose_meta_password" \
  META_DB="$compose_meta_db" \
  INFLUX_ADMIN_USER="$compose_influx_admin_user" \
  INFLUX_ADMIN_PASSWORD="$compose_influx_admin_password" \
  INFLUX_ORG="$compose_influx_org" \
  INFLUX_INTERNAL_BUCKET="$compose_influx_internal_bucket" \
  INFLUX_TOKEN="$compose_influx_token" \
  INFLUX_DATA_BUCKET="$compose_influx_data_bucket" \
  INFLUX_DATA_RETENTION="$compose_influx_data_retention" \
  INFLUX_TELEMETRY_BUCKET="$compose_influx_telemetry_bucket" \
  INFLUX_TELEMETRY_RETENTION="$compose_influx_telemetry_retention" \
  KEYCLOAK_DB_PASSWORD="$compose_keycloak_db_password" \
  KEYCLOAK_DB="$compose_keycloak_db" \
  KEYCLOAK_ADMIN_USER="$compose_keycloak_admin_user" \
  KEYCLOAK_ADMIN_PASSWORD="$compose_keycloak_admin_password" \
  CORE_JAVA_TOOL_OPTIONS="$compose_core_java_tool_options" \
  INFLUX_LATEST_RANGE="$compose_influx_latest_range" \
  PEGELHUB_IMAGE_TAG="$PEGELHUB_IMAGE_TAG" \
    docker compose \
      -p "$compose_project_name" \
      --env-file "$ENV_FILE" \
      -f "$COMPOSE_FILE" \
      "$@"
}

release_value() {
  key="$1"
  [ -f "$CURRENT_RELEASE_FILE" ] || return 0
  awk -F= -v key="$key" '$1 == key { print substr($0, length($1) + 2) }' "$CURRENT_RELEASE_FILE" | tail -n 1
}

validate_retention() {
  variable_name="$1"
  retention=$(env_value "$variable_name")
  [ -n "$retention" ] || retention="60d"

  if [ "$retention" = "0s" ] || printf '%s\n' "$retention" | grep -Eq '^[1-9][0-9]*(h|d|w)$'; then
    return
  fi

  fail "$variable_name must be 0s or a positive whole number of hours, days, or weeks."
}

validate_public_hostname() {
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
      fail "$variable_name must contain only a public DNS hostname without a scheme, port, or path."
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

validate_environment() {
  [ -f "$ENV_FILE" ] || fail "Missing deployment env file: $ENV_FILE"
  compose_project_name=$(env_value COMPOSE_PROJECT_NAME)
  [ -n "$compose_project_name" ] || fail "COMPOSE_PROJECT_NAME is required."

  case "$(env_value PEGELHUB_TLS_MODE)" in
    automatic|provided) ;;
    *) fail "PEGELHUB_TLS_MODE must be automatic or provided." ;;
  esac

  case "$(env_value PEGELHUB_TRUST_MODE)" in
    system|custom) ;;
    *) fail "PEGELHUB_TRUST_MODE must be system or custom." ;;
  esac

  validate_public_hostname PEGELHUB_FRONTEND_HOSTNAME
  validate_public_hostname PEGELHUB_API_HOSTNAME
  validate_public_hostname PEGELHUB_KEYCLOAK_HOSTNAME

  for binding_name in PEGELHUB_HTTP_BIND PEGELHUB_HTTPS_BIND; do
    binding=$(env_value "$binding_name")
    if [ -z "$binding" ]; then
      case "$binding_name" in
        PEGELHUB_HTTP_BIND) binding=80 ;;
        *) binding=443 ;;
      esac
    fi
    printf '%s\n' "$binding" \
      | grep -Eq '^([0-9]{1,5}|(127[.]0[.]0[.]1|localhost):[0-9]{1,5})$' \
      || fail "$binding_name must be a port or a loopback-address:port binding."
    port=${binding##*:}
    [ "$port" -ge 1 ] && [ "$port" -le 65535 ] \
      || fail "$binding_name port must be between 1 and 65535."
  done

  https_url_suffix=$(env_value PEGELHUB_HTTPS_URL_SUFFIX)
  case "$https_url_suffix" in
    "") ;;
    :*)
      suffix_port=${https_url_suffix#:}
      printf '%s\n' "$suffix_port" | grep -Eq '^[0-9]{1,5}$' \
        || fail "PEGELHUB_HTTPS_URL_SUFFIX must be empty or :port."
      [ "$suffix_port" -ge 1 ] && [ "$suffix_port" -le 65535 ] \
        || fail "PEGELHUB_HTTPS_URL_SUFFIX port must be between 1 and 65535."
      ;;
    *) fail "PEGELHUB_HTTPS_URL_SUFFIX must be empty or :port." ;;
  esac
  https_binding=$(env_value PEGELHUB_HTTPS_BIND)
  [ -n "$https_binding" ] || https_binding=443
  https_binding_port=${https_binding##*:}
  https_container_port=$(env_value PEGELHUB_HTTPS_CONTAINER_PORT)
  [ -n "$https_container_port" ] || https_container_port=443
  printf '%s\n' "$https_container_port" | grep -Eq '^[0-9]{1,5}$' \
    || fail "PEGELHUB_HTTPS_CONTAINER_PORT must be a port."
  [ "$https_container_port" -ge 1 ] && [ "$https_container_port" -le 65535 ] \
    || fail "PEGELHUB_HTTPS_CONTAINER_PORT must be between 1 and 65535."
  [ "$https_container_port" = "$https_binding_port" ] \
    || fail "PEGELHUB_HTTPS_CONTAINER_PORT must match the published HTTPS port."
  if [ "$https_binding_port" = 443 ]; then
    [ -z "$https_url_suffix" ] || [ "$https_url_suffix" = :443 ] \
      || fail "PEGELHUB_HTTPS_URL_SUFFIX must match PEGELHUB_HTTPS_BIND."
  else
    [ "$https_url_suffix" = ":$https_binding_port" ] \
      || fail "Alternate HTTPS bindings require a matching PEGELHUB_HTTPS_URL_SUFFIX."
  fi

  validate_retention INFLUX_DATA_RETENTION
  validate_retention INFLUX_TELEMETRY_RETENTION

  internal_bucket=$(env_value INFLUX_INTERNAL_BUCKET)
  data_bucket=$(env_value INFLUX_DATA_BUCKET)
  telemetry_bucket=$(env_value INFLUX_TELEMETRY_BUCKET)
  if [ "$internal_bucket" = "$data_bucket" ] \
    || [ "$internal_bucket" = "$telemetry_bucket" ] \
    || [ "$data_bucket" = "$telemetry_bucket" ]; then
    fail "INFLUX_INTERNAL_BUCKET, INFLUX_DATA_BUCKET, and INFLUX_TELEMETRY_BUCKET must be different."
  fi

}

load_compose_environment() {
  compose_frontend_hostname=$(env_value PEGELHUB_FRONTEND_HOSTNAME)
  compose_api_hostname=$(env_value PEGELHUB_API_HOSTNAME)
  compose_keycloak_hostname=$(env_value PEGELHUB_KEYCLOAK_HOSTNAME)
  compose_http_bind=$(env_value PEGELHUB_HTTP_BIND)
  [ -n "$compose_http_bind" ] || compose_http_bind=80
  compose_https_bind=$(env_value PEGELHUB_HTTPS_BIND)
  [ -n "$compose_https_bind" ] || compose_https_bind=443
  compose_https_url_suffix=$(env_value PEGELHUB_HTTPS_URL_SUFFIX)
  compose_https_container_port=$(env_value PEGELHUB_HTTPS_CONTAINER_PORT)
  [ -n "$compose_https_container_port" ] || compose_https_container_port=443
  compose_tls_mode=$(env_value PEGELHUB_TLS_MODE)
  compose_trust_mode=$(env_value PEGELHUB_TRUST_MODE)
  compose_tls_server_dir=$(env_value PEGELHUB_TLS_SERVER_DIR)
  [ -n "$compose_tls_server_dir" ] || compose_tls_server_dir="$CONFIG_DIR/tls/server"
  compose_trust_dir=$(env_value PEGELHUB_TRUST_DIR)
  [ -n "$compose_trust_dir" ] || compose_trust_dir="$CONFIG_DIR/tls/trust"
  [ -d "$compose_tls_server_dir" ] || fail "Missing TLS server directory: $compose_tls_server_dir"
  [ -d "$compose_trust_dir" ] || fail "Missing trust directory: $compose_trust_dir"
  if [ "$compose_tls_mode" = "provided" ]; then
    set -- "$compose_tls_server_dir"/current/*.pem
    [ -f "$1" ] || fail "Provided TLS mode requires an installed current certificate release."
  fi
  if [ "$compose_trust_mode" = "custom" ]; then
    set -- "$compose_trust_dir"/*.crt
    [ -f "$1" ] || fail "Custom trust mode requires at least one *.crt certificate."
  fi
  compose_meta_password=$(env_value META_PASSWORD)
  compose_meta_db=$(env_value META_DB)
  compose_influx_admin_user=$(env_value INFLUX_ADMIN_USER)
  compose_influx_admin_password=$(env_value INFLUX_ADMIN_PASSWORD)
  compose_influx_org=$(env_value INFLUX_ORG)
  compose_influx_internal_bucket=$(env_value INFLUX_INTERNAL_BUCKET)
  compose_influx_token=$(env_value INFLUX_TOKEN)
  compose_influx_data_bucket=$(env_value INFLUX_DATA_BUCKET)
  compose_influx_data_retention=$(env_value INFLUX_DATA_RETENTION)
  compose_influx_telemetry_bucket=$(env_value INFLUX_TELEMETRY_BUCKET)
  compose_influx_telemetry_retention=$(env_value INFLUX_TELEMETRY_RETENTION)
  compose_keycloak_db_password=$(env_value KEYCLOAK_DB_PASSWORD)
  compose_keycloak_db=$(env_value KEYCLOAK_DB)
  compose_keycloak_admin_user=$(env_value KEYCLOAK_ADMIN_USER)
  compose_keycloak_admin_password=$(env_value KEYCLOAK_ADMIN_PASSWORD)
  compose_core_java_tool_options=$(env_value CORE_JAVA_TOOL_OPTIONS)
  compose_influx_latest_range=$(env_value INFLUX_LATEST_RANGE)
}

validate_reset_request() {
  [ -n "$RESET_DATA_CONFIRMATION" ] || return 0
  [ "$RESET_DATA_CONFIRMATION" = "$compose_project_name" ] \
    || fail "--reset-data confirmation must exactly match COMPOSE_PROJECT_NAME ($compose_project_name)."
  [ "$CHECK_ONLY" = "false" ] || fail "--reset-data cannot be combined with --check."
  [ "$ROLLBACK" = "false" ] || fail "--reset-data cannot be combined with --rollback."
}

select_tag() {
  if [ "$ROLLBACK" = "true" ]; then
    [ -z "$REQUESTED_TAG" ] || fail "--rollback cannot be combined with an explicit image tag."
    tag=$(release_value PREVIOUS_PEGELHUB_IMAGE_TAG)
    [ -n "$tag" ] || fail "No previous image tag recorded in $CURRENT_RELEASE_FILE."
  elif [ -n "$REQUESTED_TAG" ]; then
    tag="$REQUESTED_TAG"
  else
    tag="${PEGELHUB_IMAGE_TAG:-$(env_value PEGELHUB_IMAGE_TAG)}"
  fi

  case "$tag" in
    ""|sha-replace-me|v0.0.0|latest)
      fail "Set PEGELHUB_IMAGE_TAG to an immutable GHCR tag such as sha-42bd19b or v0.1.0."
      ;;
  esac

  printf '%s\n' "$tag"
}

validate_compose_structure() {
  structure="$1"

  if grep -Eq '^[[:space:]]+build:' "$structure"; then
    fail "Deployment Compose contains a build section; deploy registry images instead."
  fi

  if grep -Eq 'target: (5432|5444|8081|8082|8111|9000)' "$structure" ||
     grep -Eq 'published: "?((5432|5444|8081|8082|8111|9000))"?' "$structure"; then
    fail "Deployment Compose publishes a database, actuator, InfluxDB, Keycloak, or management port."
  fi
}

validate_compose_images() {
  images=$(compose config --images)

  if ! printf '%s\n' "$images" \
    | grep -Fx "ghcr.io/viadonau/pegelhub-core:$PEGELHUB_IMAGE_TAG" >/dev/null; then
    fail "Deployment Compose does not use the requested Core image tag."
  fi

}

record_release() {
  mkdir -p "$STATE_DIR"
  previous_tag=$(release_value PEGELHUB_IMAGE_TAG)
  deployed_at=$(date -u '+%Y-%m-%dT%H:%M:%SZ')

  {
    printf 'PEGELHUB_IMAGE_TAG=%s\n' "$PEGELHUB_IMAGE_TAG"
    printf 'PREVIOUS_PEGELHUB_IMAGE_TAG=%s\n' "$previous_tag"
    printf 'DEPLOYED_AT=%s\n' "$deployed_at"
  } > "$CURRENT_RELEASE_FILE"
}

reset_data() {
  printf '%s\n' "Resetting PostgreSQL metadata and InfluxDB measurements for $compose_project_name..."
  compose rm --stop --force core-app influx-bucket-setup data-db meta-db
  docker volume rm \
    "${compose_project_name}_metastore-data" \
    "${compose_project_name}_datastore-data"
  rm -f "$CURRENT_RELEASE_FILE"
}

validate_environment
load_compose_environment
validate_reset_request
PEGELHUB_IMAGE_TAG=$(select_tag)
export PEGELHUB_IMAGE_TAG

compose_structure=$(mktemp "${TMPDIR:-/tmp}/pegelhub-compose-structure.XXXXXX")
trap cleanup EXIT
trap 'exit_on_signal 129' HUP
trap 'exit_on_signal 130' INT
trap 'exit_on_signal 143' TERM
compose config --quiet
compose config --no-interpolate > "$compose_structure"
validate_compose_structure "$compose_structure"
validate_compose_images
rm -f "$compose_structure"
compose_structure=""

printf '%s\n' "Validated deployment Compose for image tag $PEGELHUB_IMAGE_TAG."

if [ "$CHECK_ONLY" = "true" ]; then
  printf '%s\n' "Check only; no images pulled and no services changed."
  exit 0
fi

acquire_operation_lock

if [ -f "$LEGACY_RENDERED_FILE" ]; then
  rm -f "$LEGACY_RENDERED_FILE"
  printf '%s\n' "Removed the legacy rendered Compose artifact from protected deployment state."
fi

printf '%s\n' "Pulling deployment images..."
compose pull

if [ -n "$RESET_DATA_CONFIRMATION" ]; then
  reset_data
fi

if [ "$REFRESH_KEYCLOAK" = "true" ]; then
  printf '%s\n' "Recreating Keycloak for theme/config reload; realm state is unchanged..."
  compose up -d --force-recreate keycloak
fi

printf '%s\n' "Starting deployment stack..."
compose up -d

printf '%s\n' "Running deployment smoke checks..."
PEGELHUB_CONFIG_DIR="$CONFIG_DIR" PEGELHUB_STATE_DIR="$STATE_DIR" \
  PEGELHUB_ENV_FILE="$ENV_FILE" "$SCRIPT_DIR/smoke.sh"

record_release

printf '%s\n' "Deployment complete for image tag $PEGELHUB_IMAGE_TAG."
