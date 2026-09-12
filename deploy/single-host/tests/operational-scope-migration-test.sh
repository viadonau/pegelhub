#!/usr/bin/env bash
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/.." && pwd)
TEMP=$(mktemp -d)
trap 'rm -rf "$TEMP"' EXIT
export MIGRATION_CALLS="$TEMP/calls" MIGRATION_STATE="$TEMP/state"
export KCADM="$ROOT/tests/fixtures/operational-kcadm.sh"

bash "$ROOT/scripts/migrate-operational-scopes.sh"
bash "$ROOT/scripts/migrate-operational-scopes.sh" --retire-standalone-clients

[[ $(grep -c '^create ' "$MIGRATION_CALLS") == 1 ]]
if grep -Eq '(users|groups|import|update|delete)' "$MIGRATION_CALLS"; then
  printf 'Unexpected realm mutation\n' >&2
  exit 1
fi

printf 'Operational scope migration is additive and idempotent.\n'
