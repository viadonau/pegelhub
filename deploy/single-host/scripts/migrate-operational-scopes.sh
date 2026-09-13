#!/usr/bin/env bash
set -euo pipefail

# Uses an existing kcadm login; never imports or replaces realm data.
# The same helper applies to local and staging realms.
KCADM=${KCADM:-kcadm.sh}
REALM=${REALM:-pegelhub}

retire=false
if [[ ${1:-} == --retire-standalone-clients && $# == 1 ]]; then
  retire=true
elif [[ $# != 0 ]]; then
  printf 'Usage: %s [--retire-standalone-clients]\n' "$0" >&2
  exit 2
fi

command -v jq >/dev/null
command -v "$KCADM" >/dev/null

kc() {
  "$KCADM" "$@" -r "$REALM"
}

client_id() {
  kc get clients -q "clientId=$1" | jq -er --arg name "$1" \
    '[.[] | select(.clientId == $name)] | if length == 1 then .[0].id else error("Expected exactly one client: " + $name) end'
}

core=$(client_id pegelhub-core-api)
frontend=$(client_id pegelhub-frontend)
role=$(kc get "clients/$core/roles/system:admin")
scope="clients/$frontend/scope-mappings/clients/$core"

if ! kc get "$scope" | jq -e 'any(.[]; .name == "system:admin")' >/dev/null; then
  printf '%s\n' "$role" | jq '[.]' | kc create "$scope" -f -
fi
kc get "$scope" | jq -e 'any(.[]; .name == "system:admin")' >/dev/null
printf 'Frontend scope updated; no user or group roles were granted.\n'

if "$retire"; then
  for name in pegelhub-modules-frontend qa-module; do
    id=$(kc get clients -q "clientId=$name" | jq -er --arg name "$name" \
      '[.[] | select(.clientId == $name)] | if length <= 1 then (.[0].id // "") else error("Ambiguous legacy client") end')
    if [[ -n "$id" ]]; then
      kc delete "clients/$id"
    fi
  done

  printf 'Standalone QA/frontend clients retired. Other clients and realm data were preserved.\n'
fi
