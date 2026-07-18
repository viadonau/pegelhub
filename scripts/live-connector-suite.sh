#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="${PEGELHUB_REPO_ROOT:-$(cd "$SCRIPT_DIR/.." && pwd)}"
COMPOSE_FILE="$REPO_ROOT/connectors/live-test-suite/compose.yaml"

usage() {
  cat <<'USAGE'
Usage: scripts/live-connector-suite.sh [all|ftp|tstp|iec|icc]

Runs the PegelHub live connector server suite through Docker Compose.

Environment:
  LIVE_VERIFY_TIMEOUT_SECONDS  Verifier timeout in seconds. Default: 90
  KEEP_LIVE_SUITE             Set to 1 to leave containers running after failure.
USAGE
}

fail() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

compose_cmd() {
  if docker compose version >/dev/null 2>&1; then
    docker compose "$@"
  elif command -v docker-compose >/dev/null 2>&1; then
    docker-compose "$@"
  else
    fail "Docker Compose is not available"
  fi
}

scenario="${1:-all}"
case "$scenario" in
  all)
    profiles=(ftp tstp iec icc)
    ;;
  ftp|tstp|iec|icc)
    profiles=("$scenario")
    ;;
  -h|--help|help)
    usage
    exit 0
    ;;
  *)
    usage >&2
    fail "Unknown scenario: $scenario"
    ;;
esac

profile_args=()
for profile in "${profiles[@]}"; do
  profile_args+=(--profile "$profile")
done

project_name="pegelhub-live-${scenario}"
compose_base=(-f "$COMPOSE_FILE" --project-name "$project_name")

export LIVE_SCENARIO="$scenario"

cleanup() {
  status=$?
  trap - EXIT INT TERM
  if [[ "$status" -ne 0 && "${KEEP_LIVE_SUITE:-0}" == "1" ]]; then
    printf 'Leaving failed live suite containers running for inspection.\n' >&2
  else
    compose_cmd "${compose_base[@]}" "${profile_args[@]}" down --volumes --remove-orphans >/dev/null || true
  fi
  exit "$status"
}

trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

printf 'Building live connector suite images for scenario: %s\n' "$scenario"
compose_cmd "${compose_base[@]}" "${profile_args[@]}" build

set +e
compose_cmd "${compose_base[@]}" "${profile_args[@]}" up \
  --abort-on-container-exit \
  --exit-code-from verifier \
  --renew-anon-volumes
status=$?
set -e

if [[ "$status" -ne 0 ]]; then
  printf '\nLive connector suite failed. Logs follow:\n' >&2
  compose_cmd "${compose_base[@]}" "${profile_args[@]}" logs --no-color >&2 || true
fi

exit "$status"
