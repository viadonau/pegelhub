#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="${PEGELHUB_REPO_ROOT:-$(cd "$SCRIPT_DIR/.." && pwd)}"

usage() {
  cat <<'USAGE'
Usage: build-connector-image.sh <connector> [docker build args...]

Builds pegelhub-<connector>:local using the repository root as Docker context.

Connectors:
  ftp-connector
  icc-connector
  iec-connector
  ma-connector
  tstp-connector

Examples:
  scripts/build-connector-image.sh ftp-connector
  scripts/build-connector-image.sh ma-connector --no-cache

Environment:
  PEGELHUB_REPO_ROOT  Override the repository root detected from this script.
USAGE
}

fail() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

[[ $# -gt 0 ]] || {
  usage >&2
  exit 2
}

connector="$1"
shift

case "$connector" in
  ftp-connector|icc-connector|iec-connector|ma-connector|tstp-connector)
    ;;
  -h|--help|help)
    usage
    exit 0
    ;;
  *)
    usage >&2
    fail "Unknown connector: $connector"
    ;;
esac

dockerfile="$REPO_ROOT/connectors/$connector/Dockerfile"
tag="pegelhub-$connector:local"

[[ -f "$dockerfile" ]] || fail "Missing Dockerfile: $dockerfile"

printf 'Building %s from %s\n' "$tag" "$dockerfile"
docker build "$@" -f "$dockerfile" -t "$tag" "$REPO_ROOT"
