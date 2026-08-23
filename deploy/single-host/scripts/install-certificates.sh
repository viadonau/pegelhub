#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
. "$SCRIPT_DIR/../../lib/env-file.sh"

CONFIG_DIR=${PEGELHUB_CONFIG_DIR:-}
[ -n "$CONFIG_DIR" ] || { printf 'ERROR: Set PEGELHUB_CONFIG_DIR.\n' >&2; exit 1; }
ENV_FILE=${PEGELHUB_ENV_FILE:-$CONFIG_DIR/pegelhub.env}
SERVER_DIR=""
staged_dir=""

fail() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

cleanup() {
  [ -z "$staged_dir" ] || rm -rf "$staged_dir"
}
trap cleanup EXIT HUP INT TERM

reload_caddy() {
  docker compose \
    -p "$(env_value COMPOSE_PROJECT_NAME)" \
    --env-file "$ENV_FILE" \
    -f "$DEPLOY_DIR/compose.yaml" \
    exec -T caddy caddy reload --force --config /etc/caddy/Caddyfile
}

[ "$#" -ge 2 ] && [ $(( $# % 2 )) -eq 0 ] \
  || fail "Pass <name>.fullchain.pem and <name>.privkey.pem pairs."
[ -f "$ENV_FILE" ] || fail "Missing deployment env file: $ENV_FILE"
[ "$(env_value PEGELHUB_TLS_MODE)" = provided ] \
  || fail "Certificate installation requires PEGELHUB_TLS_MODE=provided."
frontend_hostname=$(env_value PEGELHUB_FRONTEND_HOSTNAME)
api_hostname=$(env_value PEGELHUB_API_HOSTNAME)
keycloak_hostname=$(env_value PEGELHUB_KEYCLOAK_HOSTNAME)
[ -n "$frontend_hostname" ] || fail "PEGELHUB_FRONTEND_HOSTNAME is required."
[ -n "$api_hostname" ] || fail "PEGELHUB_API_HOSTNAME is required."
[ -n "$keycloak_hostname" ] || fail "PEGELHUB_KEYCLOAK_HOSTNAME is required."
frontend_covered=false
api_covered=false
keycloak_covered=false

SERVER_DIR=$(env_value PEGELHUB_TLS_SERVER_DIR)
[ -n "$SERVER_DIR" ] || SERVER_DIR="$CONFIG_DIR/tls/server"
mkdir -p "$SERVER_DIR"
chmod 700 "$SERVER_DIR"
staged_dir=$(mktemp -d "$SERVER_DIR/.current.XXXXXX")
chmod 700 "$staged_dir"

while [ "$#" -gt 0 ]; do
  fullchain=$1
  private_key=$2
  shift 2

  [ -f "$fullchain" ] || fail "Missing certificate: $fullchain"
  [ -f "$private_key" ] || fail "Missing private key: $private_key"
  name=$(basename -- "$fullchain" .fullchain.pem)
  [ "$name.fullchain.pem" = "$(basename -- "$fullchain")" ] \
    || fail "Certificate names must end in .fullchain.pem."
  [ "$(basename -- "$private_key")" = "$name.privkey.pem" ] \
    || fail "Expected $name.privkey.pem next to $name.fullchain.pem."

  openssl x509 -in "$fullchain" -checkend 0 -noout >/dev/null 2>&1 \
    || fail "$name certificate is expired or invalid."
  certificate_key=$(openssl x509 -in "$fullchain" -pubkey -noout 2>/dev/null \
    | openssl pkey -pubin -outform DER 2>/dev/null \
    | openssl dgst -sha256)
  private_key_digest=$(openssl pkey -in "$private_key" -pubout -outform DER 2>/dev/null \
    | openssl dgst -sha256)
  [ -n "$certificate_key" ] && [ "$certificate_key" = "$private_key_digest" ] \
    || fail "$name certificate and private key do not match."

  if openssl verify -CAfile "$fullchain" -verify_hostname "$frontend_hostname" \
    "$fullchain" >/dev/null 2>&1; then
    frontend_covered=true
  fi
  if openssl verify -CAfile "$fullchain" -verify_hostname "$api_hostname" \
    "$fullchain" >/dev/null 2>&1; then
    api_covered=true
  fi
  if openssl verify -CAfile "$fullchain" -verify_hostname "$keycloak_hostname" \
    "$fullchain" >/dev/null 2>&1; then
    keycloak_covered=true
  fi

  cat "$fullchain" "$private_key" > "$staged_dir/$name.pem"
  chmod 600 "$staged_dir/$name.pem"
done

[ "$frontend_covered" = true ] \
  || fail "No provided certificate covers $frontend_hostname."
[ "$api_covered" = true ] \
  || fail "No provided certificate covers $api_hostname."
[ "$keycloak_covered" = true ] \
  || fail "No provided certificate covers $keycloak_hostname."

rm -rf "$SERVER_DIR/previous"
if [ -d "$SERVER_DIR/current" ]; then
  mv "$SERVER_DIR/current" "$SERVER_DIR/previous"
fi
mv "$staged_dir" "$SERVER_DIR/current"
staged_dir=""

container_id=$(docker compose \
  -p "$(env_value COMPOSE_PROJECT_NAME)" \
  --env-file "$ENV_FILE" \
  -f "$DEPLOY_DIR/compose.yaml" \
  ps -q caddy 2>/dev/null || true)
if [ -n "$container_id" ] && ! reload_caddy; then
  rm -rf "$SERVER_DIR/current"
  if [ -d "$SERVER_DIR/previous" ]; then
    mv "$SERVER_DIR/previous" "$SERVER_DIR/current"
    reload_caddy || true
  fi
  fail "Caddy rejected the provided certificates."
fi

rm -rf "$SERVER_DIR/previous"
printf 'Installed provided certificates.\n'
