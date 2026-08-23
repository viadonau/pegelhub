#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
CONFIG_DIR=${PEGELHUB_CONFIG_DIR:-}
ENV_FILE=${PEGELHUB_ENV_FILE:-}
if [ -z "$ENV_FILE" ]; then
  [ -n "$CONFIG_DIR" ] || { printf 'ERROR: Set PEGELHUB_CONFIG_DIR or PEGELHUB_ENV_FILE.\n' >&2; exit 1; }
  ENV_FILE="$CONFIG_DIR/pegelhub.env"
fi
[ -n "$CONFIG_DIR" ] || CONFIG_DIR=$(CDPATH= cd -- "$(dirname -- "$ENV_FILE")" && pwd)
STATE_DIR="${PEGELHUB_STATE_DIR:-}"
[ -n "$STATE_DIR" ] || { printf 'ERROR: Set PEGELHUB_STATE_DIR.\n' >&2; exit 1; }
RELEASE_FILE="$STATE_DIR/frontend-release.env"
LOCK_DIR="${PEGELHUB_LOCK_DIR:-$STATE_DIR/operation.lock}"

ACTIVE_IMAGE=""
PREVIOUS_IMAGE=""
DEPLOYMENT_CHANGED=false
DEPLOYMENT_COMMITTED=false
LOCK_OWNED=false
release_tmp=""
ca_bundle=""

fail() {
  printf '%s\n' "ERROR: $*" >&2
  exit 1
}

file_value() {
  file="$1"
  key="$2"
  [ -f "$file" ] || return 0
  awk -F= -v key="$key" '$1 == key { print substr($0, length($1) + 2) }' \
    "$file" | tail -n 1
}

release_value() {
  file_value "$RELEASE_FILE" "$1"
}

compose() {
  COMPOSE_IGNORE_ORPHANS=true \
  COMPOSE_REMOVE_ORPHANS=false \
  PEGELHUB_FRONTEND_IMAGE="$ACTIVE_IMAGE" \
    docker compose \
      -p "$COMPOSE_PROJECT_NAME" \
      --env-file "$ENV_FILE" \
      -f "$DEPLOY_DIR/compose.yaml" \
      -f "$DEPLOY_DIR/frontend.compose.yaml" \
      "$@"
}

smoke_frontend() {
  attempts="${FRONTEND_SMOKE_RETRIES:-30}"
  delay="${FRONTEND_SMOKE_RETRY_DELAY_SECONDS:-5}"

  for url in \
    "https://$PEGELHUB_FRONTEND_HOSTNAME/" \
    "https://$PEGELHUB_FRONTEND_HOSTNAME/api/v1/measurements/system-time"; do
    attempt=1
    until curl -fsS "$url" >/dev/null; do
      [ "$attempt" -lt "$attempts" ] || return 1
      sleep "$delay"
      attempt=$((attempt + 1))
    done
  done
}

restore_previous_release() {
  if [ -n "$PREVIOUS_IMAGE" ]; then
    printf '%s\n' "Restoring $PREVIOUS_IMAGE..." >&2
    ACTIVE_IMAGE="$PREVIOUS_IMAGE"
    compose up -d --no-deps --force-recreate \
      --wait --wait-timeout "${FRONTEND_HEALTH_TIMEOUT_SECONDS:-150}" frontend \
      && smoke_frontend
    return
  fi

  printf '%s\n' "Removing failed first frontend release..." >&2
  compose rm -sf frontend
}

release_lock() {
  [ "$LOCK_OWNED" = "true" ] || return 0
  LOCK_OWNED=false
  rmdir "$LOCK_DIR" >/dev/null 2>&1 || true
}

cleanup() {
  status=$?
  trap - EXIT HUP INT TERM
  [ -z "$release_tmp" ] || rm -f "$release_tmp"

  if [ "$DEPLOYMENT_CHANGED" = "true" ] \
    && [ "$DEPLOYMENT_COMMITTED" = "false" ]; then
    if ! restore_previous_release; then
      printf '%s\n' "ERROR: Frontend rollback failed." >&2
      status=1
    fi
  fi

  [ -z "$ca_bundle" ] || rm -f "$ca_bundle"

  release_lock
  exit "$status"
}

acquire_lock() {
  mkdir -p "$STATE_DIR"
  if ! mkdir "$LOCK_DIR" 2>/dev/null; then
    fail "Another deployment operation is active."
  fi
  LOCK_OWNED=true
  chmod 700 "$LOCK_DIR"
}

record_release() {
  previous_image="$PREVIOUS_IMAGE"
  if [ "$ACTIVE_IMAGE" = "$PREVIOUS_IMAGE" ]; then
    previous_image=$(release_value PREVIOUS_FRONTEND_IMAGE)
  fi

  mkdir -p "$STATE_DIR"
  release_tmp=$(mktemp "$STATE_DIR/frontend-release.XXXXXX")
  chmod 600 "$release_tmp"
  {
    printf 'FRONTEND_IMAGE=%s\n' "$ACTIVE_IMAGE"
    printf 'PREVIOUS_FRONTEND_IMAGE=%s\n' "$previous_image"
    printf 'DEPLOYED_AT=%s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  } > "$release_tmp"
  mv "$release_tmp" "$RELEASE_FILE"
  release_tmp=""
}

[ "$#" -eq 1 ] || fail "Pass an immutable frontend image or --rollback."

trap cleanup EXIT
trap 'exit 129' HUP
trap 'exit 130' INT
trap 'exit 143' TERM

COMPOSE_PROJECT_NAME=$(file_value "$ENV_FILE" COMPOSE_PROJECT_NAME)
PEGELHUB_FRONTEND_HOSTNAME=$(file_value "$ENV_FILE" PEGELHUB_FRONTEND_HOSTNAME)
[ -n "$COMPOSE_PROJECT_NAME" ] || fail "COMPOSE_PROJECT_NAME is required."
[ -n "$PEGELHUB_FRONTEND_HOSTNAME" ] || fail "PEGELHUB_FRONTEND_HOSTNAME is required."
if [ "$(file_value "$ENV_FILE" PEGELHUB_TRUST_MODE)" = custom ]; then
  trust_dir=$(file_value "$ENV_FILE" PEGELHUB_TRUST_DIR)
  [ -n "$trust_dir" ] || trust_dir="$CONFIG_DIR/tls/trust"
  ca_bundle=$(mktemp "${TMPDIR:-/tmp}/pegelhub-frontend-ca.XXXXXX")
  "$SCRIPT_DIR/build-ca-bundle.sh" "$trust_dir" "$ca_bundle"
  CURL_CA_BUNDLE=$ca_bundle
  export CURL_CA_BUNDLE
fi

acquire_lock
PREVIOUS_IMAGE=$(release_value FRONTEND_IMAGE)

pull_image=true
if [ "$1" = "--rollback" ]; then
  ACTIVE_IMAGE=$(release_value PREVIOUS_FRONTEND_IMAGE)
  [ -n "$ACTIVE_IMAGE" ] || fail "No previous frontend release is recorded."
  pull_image=false
else
  ACTIVE_IMAGE="$1"
fi

printf '%s\n' "$ACTIVE_IMAGE" \
  | grep -Eq '^ghcr[.]io/viadonau/pegelhub-frontend@sha256:[0-9a-f]{64}$' \
  || fail "Frontend releases must use the expected GHCR image by digest."

if [ "$pull_image" = "true" ]; then
  printf '%s\n' "Pulling $ACTIVE_IMAGE..."
  compose pull frontend
fi

DEPLOYMENT_CHANGED=true
printf '%s\n' "Activating $ACTIVE_IMAGE..."
compose up -d --no-deps --force-recreate \
  --wait --wait-timeout "${FRONTEND_HEALTH_TIMEOUT_SECONDS:-150}" frontend
smoke_frontend

# Keep activation and release state indivisible from the signal handler's view.
trap '' HUP INT TERM
record_release
DEPLOYMENT_COMMITTED=true
trap 'exit 129' HUP
trap 'exit 130' INT
trap 'exit 143' TERM

printf '%s\n' "Frontend release complete: $ACTIVE_IMAGE"
