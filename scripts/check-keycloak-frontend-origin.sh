#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="${PEGELHUB_REPO_ROOT:-$(cd "$SCRIPT_DIR/.." && pwd)}"
REALM_IMPORT="${REALM_IMPORT:-$REPO_ROOT/core/docker/keycloak/import/pegelhub-realm.json}"
FRONTEND_CLIENT_ID="${FRONTEND_CLIENT_ID:-pegelhub-frontend}"
EXPECTED_ORIGIN="${PEGELHUB_FRONTEND_URL:-}"
RUNNING_ORIGIN="${1:-$EXPECTED_ORIGIN}"

usage() {
  cat <<'USAGE'
Usage: PEGELHUB_FRONTEND_URL=<imported-origin> scripts/check-keycloak-frontend-origin.sh [running-origin]

Checks that the local Keycloak realm import template uses PEGELHUB_FRONTEND_URL
for the browser client and that the running Angular origin matches the origin
used when the realm was imported.

Examples:
  PEGELHUB_FRONTEND_URL=http://localhost:4200 scripts/check-keycloak-frontend-origin.sh
  PEGELHUB_FRONTEND_URL=http://localhost:4201 scripts/check-keycloak-frontend-origin.sh http://localhost:4201
USAGE
}

fail() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

case "${1:-}" in
  -h|--help|help)
    usage
    exit 0
    ;;
esac

[[ -n "$EXPECTED_ORIGIN" ]] || fail "Set PEGELHUB_FRONTEND_URL to the origin used for the Keycloak realm import."
[[ -n "$RUNNING_ORIGIN" ]] || fail "Pass the running frontend origin or set PEGELHUB_FRONTEND_URL."
[[ -f "$REALM_IMPORT" ]] || fail "Missing realm import: $REALM_IMPORT"
command -v python3 >/dev/null 2>&1 || fail "python3 is required to parse the Keycloak realm import."

python3 - "$REALM_IMPORT" "$FRONTEND_CLIENT_ID" "$EXPECTED_ORIGIN" "$RUNNING_ORIGIN" <<'PY'
import json
import sys
from urllib.parse import urlparse

realm_import, client_id, expected_origin, running_origin = sys.argv[1:]

def fail(message):
    print(f"ERROR: {message}", file=sys.stderr)
    sys.exit(1)

def normalize_origin(value):
    parsed = urlparse(value)
    if parsed.scheme not in {"http", "https"} or not parsed.netloc:
        fail(f"Invalid origin: {value}")
    path = parsed.path.rstrip("/")
    if path:
        fail(f"Origin must not include a path: {value}")
    return f"{parsed.scheme}://{parsed.netloc}"

with open(realm_import, "r", encoding="utf-8") as handle:
    realm = json.load(handle)

client = next((item for item in realm.get("clients", []) if item.get("clientId") == client_id), None)
if client is None:
    fail(f"Client {client_id!r} was not found in {realm_import}.")

expected_template = "${PEGELHUB_FRONTEND_URL}"
checks = {
    "rootUrl": client.get("rootUrl"),
    "redirectUris": client.get("redirectUris"),
    "webOrigins": client.get("webOrigins"),
}

if checks["rootUrl"] != expected_template:
    fail(f"{client_id}.rootUrl must be {expected_template!r}, got {checks['rootUrl']!r}.")
if checks["redirectUris"] != [f"{expected_template}/*"]:
    fail(f"{client_id}.redirectUris must be ['{expected_template}/*'], got {checks['redirectUris']!r}.")
if checks["webOrigins"] != [expected_template]:
    fail(f"{client_id}.webOrigins must be ['{expected_template}'], got {checks['webOrigins']!r}.")

expected = normalize_origin(expected_origin)
running = normalize_origin(running_origin)

if expected != running:
    fail(
        "Running frontend origin does not match the Keycloak import origin: "
        f"running={running}, imported={expected}. Recreate/update the local realm or start Angular on {expected}."
    )

print(f"OK: {client_id} realm import is frontend-origin templated.")
print(f"OK: running frontend origin matches imported origin: {running}")
PY
