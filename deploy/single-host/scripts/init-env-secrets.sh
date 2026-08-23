#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
. "$SCRIPT_DIR/../../lib/env-file.sh"
CONFIG_DIR=${PEGELHUB_CONFIG_DIR:-}
[ -n "$CONFIG_DIR" ] || { printf 'ERROR: Set PEGELHUB_CONFIG_DIR.\n' >&2; exit 1; }
ENV_FILE="${PEGELHUB_ENV_FILE:-$CONFIG_DIR/pegelhub.env}"
ENV_EXAMPLE_FILE="${PEGELHUB_ENV_EXAMPLE_FILE:-$DEPLOY_DIR/pegelhub.env.example}"

fail() {
  printf '%s\n' "ERROR: $*" >&2
  exit 1
}

generate_secret() {
  command -v openssl >/dev/null 2>&1 || fail "openssl is required to generate deployment secrets."
  openssl rand -base64 32 | tr -d '\n'
}

should_initialize() {
  value="$1"
  case "$value" in
    ""|replace-with-staging-*|replace-with-pegelhub-*|CHANGE_ME|changeme)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

replace_or_append() {
  key="$1"
  value="$2"
  tmp_file=$(mktemp "$CONFIG_DIR/.env.XXXXXX")

  awk -v key="$key" -v value="$value" '
    BEGIN { found = 0 }
    $0 ~ "^[[:space:]]*" key "=" {
      print key "=" value
      found = 1
      next
    }
    { print }
    END {
      if (!found) {
        print key "=" value
      }
    }
  ' "$ENV_FILE" > "$tmp_file"

  mv "$tmp_file" "$ENV_FILE"
  chmod 600 "$ENV_FILE"
}

initialize_secret() {
  key="$1"
  current_value=$(env_value "$key")

  if should_initialize "$current_value"; then
    replace_or_append "$key" "$(generate_secret)"
    printf '%s\n' "initialized $key"
  else
    printf '%s\n' "kept $key"
  fi
}

umask 077

if [ ! -f "$ENV_FILE" ]; then
  [ -f "$ENV_EXAMPLE_FILE" ] || fail "Missing $ENV_FILE and $ENV_EXAMPLE_FILE."
  cp "$ENV_EXAMPLE_FILE" "$ENV_FILE"
fi

chmod 600 "$ENV_FILE"

initialize_secret META_PASSWORD
initialize_secret INFLUX_ADMIN_PASSWORD
initialize_secret INFLUX_TOKEN
initialize_secret KEYCLOAK_DB_PASSWORD
initialize_secret KEYCLOAK_ADMIN_PASSWORD
