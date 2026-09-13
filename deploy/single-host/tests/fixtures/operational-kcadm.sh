#!/usr/bin/env bash
set -euo pipefail

printf '%s\n' "$*" >> "$MIGRATION_CALLS"

case "$1 $2" in
  'get clients')
    case "$4" in
      clientId=pegelhub-core-api)
        printf '[{"id":"core","clientId":"pegelhub-core-api"}]'
        ;;
      clientId=pegelhub-frontend)
        printf '[{"id":"frontend","clientId":"pegelhub-frontend"}]'
        ;;
      *)
        printf '[]'
        ;;
    esac
    ;;
  'get clients/core/roles/system:admin')
    printf '{"id":"admin","name":"system:admin"}'
    ;;
  'get clients/frontend/scope-mappings/clients/core')
    if [[ -f "$MIGRATION_STATE" ]]; then
      printf '[{"id":"admin","name":"system:admin"}]'
    else
      printf '[]'
    fi
    ;;
  'create clients/frontend/scope-mappings/clients/core')
    jq -e 'length == 1 and .[0].name == "system:admin"' > /dev/null
    touch "$MIGRATION_STATE"
    ;;
  *)
    printf 'Unexpected mutation: %s\n' "$*" >&2
    exit 1
    ;;
esac
