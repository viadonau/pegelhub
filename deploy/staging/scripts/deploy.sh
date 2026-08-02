#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
COMPOSE_FILE="$DEPLOY_DIR/compose.yaml"
ENV_FILE="${PEGELHUB_STAGING_ENV_FILE:-$DEPLOY_DIR/.env}"
STATE_DIR="$DEPLOY_DIR/state"
CURRENT_RELEASE_FILE="$STATE_DIR/current-release.env"
LEGACY_RENDERED_FILE="$STATE_DIR/compose.rendered.yaml"
LOCK_DIR="$STATE_DIR/keycloak-bootstrap.lock"

CHECK_ONLY=false
ROLLBACK=false
REFRESH_KEYCLOAK=false
REQUESTED_TAG=""
LOCK_OWNED=false
LOCK_TOKEN=""
compose_structure=""

usage() {
  cat <<USAGE
Usage:
  $0 [--check] [--refresh-keycloak] <image-tag>
  $0 --rollback

Examples:
  $0 --check sha-42bd19b
  $0 --refresh-keycloak sha-42bd19b
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

acquire_operation_lock() {
  mkdir -p "$STATE_DIR"
  if ! mkdir "$LOCK_DIR" 2>/dev/null; then
    fail "Another staging deploy or Keycloak bootstrap operation is active."
  fi
  chmod 700 "$LOCK_DIR"
  LOCK_TOKEN="deploy-$$-$(date +%s)"
  printf '%s\n' "$LOCK_TOKEN" > "$LOCK_DIR/owner"
  chmod 600 "$LOCK_DIR/owner"
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
  COMPOSE_PROFILES="$compose_profiles" \
  FTP_CONFIG_DIR="$compose_ftp_config_dir" \
  PEGELHUB_FRONTEND_HOSTNAME="$compose_frontend_hostname" \
  PEGELHUB_API_HOSTNAME="$compose_api_hostname" \
  PEGELHUB_KEYCLOAK_HOSTNAME="$compose_keycloak_hostname" \
  PEGELHUB_FRONTEND_IMAGE="$compose_frontend_image" \
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
  FLYWAY_BASELINE_ON_MIGRATE="$compose_flyway_baseline" \
  INFLUX_LATEST_RANGE="$compose_influx_latest_range" \
  FTP_JAVA_TOOL_OPTIONS="$compose_ftp_java_tool_options" \
  PEGELHUB_IMAGE_TAG="$PEGELHUB_IMAGE_TAG" \
    docker compose \
      -p "$compose_project_name" \
      --env-file "$ENV_FILE" \
      -f "$COMPOSE_FILE" \
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

release_value() {
  key="$1"
  [ -f "$CURRENT_RELEASE_FILE" ] || return 0
  awk -F= -v key="$key" '$1 == key { print substr($0, length($1) + 2) }' "$CURRENT_RELEASE_FILE" | tail -n 1
}

resolve_path() {
  path="$1"
  case "$path" in
    /*) printf '%s\n' "$path" ;;
    *) printf '%s\n' "$DEPLOY_DIR/$path" ;;
  esac
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

validate_environment() {
  [ -f "$ENV_FILE" ] || fail "Missing $ENV_FILE. Copy .env.example to .env and fill it on the staging host."

  environment=$(env_value PEGELHUB_ENVIRONMENT)
  marker=$(env_value PEGELHUB_DEPLOY_MARKER)
  [ "$environment" = "staging" ] || fail "PEGELHUB_ENVIRONMENT must be staging."
  [ "$marker" = "pegelhub-staging" ] || fail "PEGELHUB_DEPLOY_MARKER must be pegelhub-staging."

  compose_project_name=$(env_value COMPOSE_PROJECT_NAME)
  case "$compose_project_name" in
    pegelhub-staging|pegelhub-keycloak-test-*) ;;
    *) fail "COMPOSE_PROJECT_NAME must identify the staging or disposable Keycloak test project." ;;
  esac

  validate_public_hostname PEGELHUB_FRONTEND_HOSTNAME
  validate_public_hostname PEGELHUB_API_HOSTNAME
  validate_public_hostname PEGELHUB_KEYCLOAK_HOSTNAME

  case "$(env_value FLYWAY_BASELINE_ON_MIGRATE)" in
    ""|true|false) ;;
    *) fail "FLYWAY_BASELINE_ON_MIGRATE must be true or false." ;;
  esac

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

  ftp_config_dir=$(env_value FTP_CONFIG_DIR)
  [ -n "$ftp_config_dir" ] || ftp_config_dir="./ftp-config"
  ftp_config_dir=$(resolve_path "$ftp_config_dir")
  [ -d "$ftp_config_dir" ] || fail "FTP config directory does not exist: $ftp_config_dir"
  [ -f "$ftp_config_dir/connector.yaml" ] || fail "Missing FTP connector.yaml in $ftp_config_dir"
  [ -d "$ftp_config_dir/mappings" ] || fail "Missing FTP mappings directory in $ftp_config_dir"

  compose_profiles=$(env_value COMPOSE_PROFILES)
  if printf '%s' "$compose_profiles" | grep -Eq '(^|.*,)[[:space:]]*frontend[[:space:]]*(,.*|$)'; then
    frontend_image=$(env_value PEGELHUB_FRONTEND_IMAGE)
    case "$frontend_image" in
      ""|*sha-replace-me*|*latest)
        fail "COMPOSE_PROFILES enables frontend, but PEGELHUB_FRONTEND_IMAGE is missing, still a placeholder, or uses latest."
        ;;
    esac
  fi
}

load_compose_environment() {
  compose_profiles=$(env_value COMPOSE_PROFILES)
  compose_ftp_config_dir=$ftp_config_dir
  compose_frontend_hostname=$(env_value PEGELHUB_FRONTEND_HOSTNAME)
  compose_api_hostname=$(env_value PEGELHUB_API_HOSTNAME)
  compose_keycloak_hostname=$(env_value PEGELHUB_KEYCLOAK_HOSTNAME)
  compose_frontend_image=$(env_value PEGELHUB_FRONTEND_IMAGE)
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
  compose_flyway_baseline=$(env_value FLYWAY_BASELINE_ON_MIGRATE)
  compose_influx_latest_range=$(env_value INFLUX_LATEST_RANGE)
  compose_ftp_java_tool_options=$(env_value FTP_JAVA_TOOL_OPTIONS)
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
    fail "Staging Compose contains a build section. Staging must deploy registry images."
  fi

  if grep -Eq 'target: (5432|5444|8081|8082|8111|9000)' "$structure" ||
     grep -Eq 'published: "?((5432|5444|8081|8082|8111|9000))"?' "$structure"; then
    fail "Staging Compose publishes a database, actuator, InfluxDB, Keycloak, or management port."
  fi
}

validate_compose_images() {
  images=$(compose config --images)

  if ! printf '%s\n' "$images" \
    | grep -Fx "ghcr.io/viadonau/pegelhub-core:$PEGELHUB_IMAGE_TAG" >/dev/null; then
    fail "Staging Compose does not use the requested Core image tag."
  fi

  if ! printf '%s\n' "$images" \
    | grep -Fx "ghcr.io/viadonau/pegelhub-ftp-connector:$PEGELHUB_IMAGE_TAG" >/dev/null; then
    fail "Staging Compose does not use the requested FTP connector image tag."
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

validate_environment
load_compose_environment
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

printf '%s\n' "Validated staging Compose for image tag $PEGELHUB_IMAGE_TAG."

if [ "$CHECK_ONLY" = "true" ]; then
  printf '%s\n' "Check only; no images pulled and no services changed."
  exit 0
fi

acquire_operation_lock

if [ -f "$LEGACY_RENDERED_FILE" ]; then
  rm -f "$LEGACY_RENDERED_FILE"
  printf '%s\n' "Removed the legacy rendered Compose artifact from protected staging state."
fi

printf '%s\n' "Pulling staging images..."
compose pull

if [ "$REFRESH_KEYCLOAK" = "true" ]; then
  printf '%s\n' "Recreating staging Keycloak for theme/config reload; realm state is unchanged..."
  compose up -d --force-recreate keycloak
fi

printf '%s\n' "Starting staging stack..."
compose up -d

printf '%s\n' "Running staging smoke checks..."
PEGELHUB_STAGING_ENV_FILE="$ENV_FILE" "$SCRIPT_DIR/smoke.sh"

record_release

printf '%s\n' "Staging deploy complete for image tag $PEGELHUB_IMAGE_TAG."
