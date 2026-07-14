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
  LIVE_SUITE_NO_BUILD         Set to 1 to skip image builds and reuse local images.
  LIVE_SUITE_VERBOSE          Set to 1 to stream all service logs during the run.
  LIVE_SUITE_PROJECT_NAME     Override the Docker Compose project name.
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

compose_supports_up_flag() {
  compose_cmd up --help 2>/dev/null | grep -q -- "$1"
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

[[ -f "$COMPOSE_FILE" ]] || fail "Compose file not found: $COMPOSE_FILE"
command -v docker >/dev/null 2>&1 || fail "Docker is not available"
docker info >/dev/null 2>&1 || fail "Docker daemon is not reachable"

profile_args=()
for profile in "${profiles[@]}"; do
  profile_args+=(--profile "$profile")
done

project_name="${LIVE_SUITE_PROJECT_NAME:-pegelhub-live-${scenario}}"
compose_base=(-f "$COMPOSE_FILE" --project-name "$project_name")
should_cleanup=0

export LIVE_SCENARIO="$scenario"

cleanup() {
  if [[ "$should_cleanup" == "1" && "${KEEP_LIVE_SUITE:-0}" != "1" ]]; then
    compose_cmd "${compose_base[@]}" "${profile_args[@]}" down --volumes --remove-orphans >/dev/null || true
  fi
}
trap cleanup EXIT

if [[ "${LIVE_SUITE_NO_BUILD:-0}" == "1" ]]; then
  printf 'Skipping image build for scenario: %s\n' "$scenario"
else
  printf 'Building live connector suite images for scenario: %s\n' "$scenario"
  compose_cmd "${compose_base[@]}" "${profile_args[@]}" build
fi

up_args=(
  up
  --abort-on-container-exit
  --exit-code-from verifier
  --renew-anon-volumes
)

if [[ "${LIVE_SUITE_VERBOSE:-0}" != "1" ]] && compose_supports_up_flag "--attach"; then
  up_args+=(--attach verifier)
fi

set +e
should_cleanup=1
compose_cmd "${compose_base[@]}" "${profile_args[@]}" "${up_args[@]}"
status=$?
set -e

if [[ "$status" -ne 0 ]]; then
  printf '\nLive connector suite failed. Logs follow:\n' >&2
  compose_cmd "${compose_base[@]}" "${profile_args[@]}" logs --no-color >&2 || true
fi

if [[ "$status" -ne 0 && "${KEEP_LIVE_SUITE:-0}" == "1" ]]; then
  printf 'Leaving Compose project %s running for inspection.\n' "$project_name" >&2
fi

exit "$status"
