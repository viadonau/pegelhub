#!/usr/bin/env bash
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/.." && pwd)
TEMP=$(mktemp -d)
trap 'rm -rf "$TEMP"' EXIT

export REAL_DOCKER
REAL_DOCKER=$(command -v docker)
export COMPOSE_CALLS="$TEMP/calls"

mkdir "$TEMP/bin" "$TEMP/state"
mkdir -p "$TEMP/tls/server" "$TEMP/tls/trust"
printf 'pegelhub: {}\n' > "$TEMP/operations.yaml"
sed -e 's/example.com/operations.pegelhub.at/g' -e 's/sha-replace-me/sha-test/' \
    -e "s|^PEGELHUB_OPERATIONAL_CONFIG_FILE=.*|PEGELHUB_OPERATIONAL_CONFIG_FILE=$TEMP/operations.yaml|" \
    "$ROOT/pegelhub.env.example" > "$TEMP/pegelhub.env"
printf 'PREVIOUS_PEGELHUB_IMAGE_TAG=sha-previous\n' > "$TEMP/state/current-release.env"

cat > "$TEMP/bin/docker" <<'SH'
#!/bin/sh
printf '%s\n' "$*" >> "$COMPOSE_CALLS"
exec "$REAL_DOCKER" "$@"
SH
chmod +x "$TEMP/bin/docker"
export PATH="$TEMP/bin:$PATH" PEGELHUB_ENV_FILE="$TEMP/pegelhub.env" PEGELHUB_STATE_DIR="$TEMP/state"

"$ROOT/scripts/deploy.sh" --check
"$ROOT/scripts/deploy.sh" --rollback --check

if grep '^compose ' "$COMPOSE_CALLS" | grep -v 'operational.compose.yaml'; then
  printf 'Canonical deployment dropped operational configuration\n' >&2
  exit 1
fi

PEGELHUB_OPERATIONAL_CONFIG_FILE="$TEMP/operations.yaml" docker compose \
  --env-file "$TEMP/pegelhub.env" -f "$ROOT/compose.yaml" -f "$ROOT/operational.compose.yaml" \
  config --format json | jq -e --arg path "$TEMP/operations.yaml" '
    .services["core-app"].environment.SPRING_CONFIG_ADDITIONAL_LOCATION == "file:/run/pegelhub/operations.yaml"
    and any(.services["core-app"].volumes[]; .source == $path and .read_only == true)
  ' >/dev/null

printf 'Deploy and rollback preserve operational configuration.\n'
