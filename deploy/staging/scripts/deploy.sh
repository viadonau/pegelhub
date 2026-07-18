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

  ingress_mode=$(env_value PEGELHUB_INGRESS_MODE)
  [ -n "$ingress_mode" ] || ingress_mode=public
  case "$ingress_mode" in
    public)
      ;;
    company)
      [ "$(env_value PEGELHUB_COMPANY_CERT_PUBLICLY_TRUSTED)" = "true" ] ||
        fail "company ingress requires PEGELHUB_COMPANY_CERT_PUBLICLY_TRUSTED=true; private CA certificates are not supported by the current JVM containers."
      validate_company_certificate
      ;;
    *)
      fail "PEGELHUB_INGRESS_MODE must be public or company."
      ;;
  esac

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

validate_company_certificate() {
  certificate="$DEPLOY_DIR/certs/fullchain.pem"
  private_key="$DEPLOY_DIR/certs/privkey.pem"
  [ -r "$certificate" ] || fail "company ingress requires readable $certificate"
  [ -r "$private_key" ] || fail "company ingress requires readable $private_key"
  command -v openssl >/dev/null 2>&1 || fail "openssl is required to validate company ingress certificates."

  openssl x509 -in "$certificate" -noout >/dev/null 2>&1 || fail "Invalid company certificate: $certificate"
  openssl pkey -in "$private_key" -noout >/dev/null 2>&1 || fail "Invalid company private key: $private_key"
  openssl x509 -in "$certificate" -checkend 86400 -noout >/dev/null 2>&1 ||
    fail "Company certificate is expired or expires within 24 hours."

  certificate_key=$(openssl x509 -in "$certificate" -pubkey -noout |
    openssl pkey -pubin -outform DER 2>/dev/null |
    openssl dgst -sha256)
  private_key_value=$(openssl pkey -in "$private_key" -pubout -outform DER 2>/dev/null |
    openssl dgst -sha256)
  [ "$certificate_key" = "$private_key_value" ] || fail "Company certificate and private key do not match."

  for hostname_key in PEGELHUB_FRONTEND_HOSTNAME PEGELHUB_API_HOSTNAME PEGELHUB_KEYCLOAK_HOSTNAME; do
    hostname=$(env_value "$hostname_key")
    openssl x509 -in "$certificate" -checkhost "$hostname" -noout >/dev/null 2>&1 ||
      fail "Company certificate does not cover $hostname_key=$hostname."
  done
}

validate_caddy_config() {
  compose run --rm --no-deps -e CADDY_RENDER_ONLY=true caddy >/dev/null
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
validate_caddy_config

printf '%s\n' "Validated staging Compose and Caddy ingress for image tag $PEGELHUB_IMAGE_TAG."

if [ "$CHECK_ONLY" = "true" ]; then
  printf '%s\n' "Check only; no application images pulled and no persistent services changed."
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
