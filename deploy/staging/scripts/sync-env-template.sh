#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
ENV_FILE="${PEGELHUB_STAGING_ENV_FILE:-$DEPLOY_DIR/.env}"
ENV_EXAMPLE_FILE="${PEGELHUB_STAGING_ENV_EXAMPLE_FILE:-$DEPLOY_DIR/.env.example}"

fail() {
  printf '%s\n' "ERROR: $*" >&2
  exit 1
}

[ -f "$ENV_EXAMPLE_FILE" ] || fail "Missing $ENV_EXAMPLE_FILE."

umask 077

if [ ! -f "$ENV_FILE" ]; then
  cp "$ENV_EXAMPLE_FILE" "$ENV_FILE"
  chmod 600 "$ENV_FILE"
  printf '%s\n' "created $ENV_FILE"
  exit 0
fi

append_file=$(mktemp "$DEPLOY_DIR/.env.append.XXXXXX")
added_file=$(mktemp "$DEPLOY_DIR/.env.added.XXXXXX")
cleanup() {
  rm -f "$append_file" "$added_file"
}
trap cleanup EXIT

awk -F= -v added_file="$added_file" '
  FNR == NR {
    if ($0 !~ /^[[:space:]]*(#|$)/ && $1 ~ /^[A-Za-z_][A-Za-z0-9_]*$/) {
      existing[$1] = 1
    }
    next
  }

  $0 ~ /^[[:space:]]*(#|$)/ {
    pending = pending $0 ORS
    next
  }

  $1 ~ /^[A-Za-z_][A-Za-z0-9_]*$/ {
    key = $1
    if (!(key in existing)) {
      if (pending != "") {
        printf "%s", pending
      }
      print $0
      print "added " key > added_file
    }
    pending = ""
    next
  }

  {
    pending = ""
  }
' "$ENV_FILE" "$ENV_EXAMPLE_FILE" > "$append_file"

if [ -s "$append_file" ]; then
  {
    printf '\n'
    printf '# -------------------------------------------------------------------\n'
    printf '# Added from .env.example by sync-env-template.sh\n'
    printf '# Review placeholders before deploying.\n'
    printf '# -------------------------------------------------------------------\n'
    cat "$append_file"
  } >> "$ENV_FILE"
  chmod 600 "$ENV_FILE"
  cat "$added_file"
else
  printf '%s\n' "env template already in sync"
fi
