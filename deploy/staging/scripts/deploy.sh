#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
COMPOSE_FILE="$DEPLOY_DIR/compose.yaml"
ENV_FILE="$DEPLOY_DIR/.env"
STATE_DIR="$DEPLOY_DIR/state"
CURRENT_RELEASE_FILE="$STATE_DIR/current-release.env"
RENDERED_FILE="$STATE_DIR/compose.rendered.yaml"

CHECK_ONLY=false
ROLLBACK=false
REFRESH_KEYCLOAK=false
REQUESTED_TAG=""

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
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"
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

validate_environment() {
  [ -f "$ENV_FILE" ] || fail "Missing $ENV_FILE. Copy .env.example to .env and fill it on the staging host."

  environment=$(env_value PEGELHUB_ENVIRONMENT)
  marker=$(env_value PEGELHUB_DEPLOY_MARKER)
  [ "$environment" = "staging" ] || fail "PEGELHUB_ENVIRONMENT must be staging."
  [ "$marker" = "pegelhub-staging" ] || fail "PEGELHUB_DEPLOY_MARKER must be pegelhub-staging."

  [ -n "$(env_value PEGELHUB_FRONTEND_HOSTNAME)" ] || fail "PEGELHUB_FRONTEND_HOSTNAME is missing."
  [ -n "$(env_value PEGELHUB_API_HOSTNAME)" ] || fail "PEGELHUB_API_HOSTNAME is missing."
  [ -n "$(env_value PEGELHUB_KEYCLOAK_HOSTNAME)" ] || fail "PEGELHUB_KEYCLOAK_HOSTNAME is missing."

  ftp_config_dir=$(env_value FTP_CONFIG_DIR)
  [ -n "$ftp_config_dir" ] || ftp_config_dir="./ftp-config"
  ftp_config_dir=$(resolve_path "$ftp_config_dir")
  [ -d "$ftp_config_dir" ] || fail "FTP config directory does not exist: $ftp_config_dir"
  [ -f "$ftp_config_dir/connector.properties" ] || fail "Missing FTP connector.properties in $ftp_config_dir"
  [ -f "$ftp_config_dir/pegelhub.yaml" ] || fail "Missing FTP pegelhub.yaml in $ftp_config_dir"

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

validate_rendered_config() {
  rendered="$1"

  if grep -Eq '^[[:space:]]+build:' "$rendered"; then
    fail "Rendered staging Compose contains a build section. Staging must deploy registry images."
  fi

  if grep -Eq 'target: (5432|5444|8081|8082|8111|9000)' "$rendered" ||
     grep -Eq 'published: "?((5432|5444|8081|8082|8111|9000))"?' "$rendered"; then
    fail "Rendered staging Compose publishes a database, actuator, InfluxDB, Keycloak, or management port."
  fi

  if ! grep -q "ghcr.io/viadonau/pegelhub-core:$PEGELHUB_IMAGE_TAG" "$rendered"; then
    fail "Rendered staging Compose does not use the requested Core image tag."
  fi

  if ! grep -q "ghcr.io/viadonau/pegelhub-ftp-connector:$PEGELHUB_IMAGE_TAG" "$rendered"; then
    fail "Rendered staging Compose does not use the requested FTP connector image tag."
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
PEGELHUB_IMAGE_TAG=$(select_tag)
export PEGELHUB_IMAGE_TAG

mkdir -p "$STATE_DIR"
compose config > "$RENDERED_FILE"
validate_rendered_config "$RENDERED_FILE"

printf '%s\n' "Validated staging Compose for image tag $PEGELHUB_IMAGE_TAG."

if [ "$CHECK_ONLY" = "true" ]; then
  printf '%s\n' "Check only; no images pulled and no services changed."
  exit 0
fi

printf '%s\n' "Pulling staging images..."
compose pull

if [ "$REFRESH_KEYCLOAK" = "true" ]; then
  printf '%s\n' "Refreshing staging Keycloak..."
  compose up -d --force-recreate keycloak
fi

printf '%s\n' "Starting staging stack..."
compose up -d

printf '%s\n' "Running staging smoke checks..."
"$SCRIPT_DIR/smoke.sh"

record_release

printf '%s\n' "Staging deploy complete for image tag $PEGELHUB_IMAGE_TAG."
