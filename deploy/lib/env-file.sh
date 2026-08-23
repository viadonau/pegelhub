#!/bin/sh

# Shared parser for the protected deployment env file. Sourcing scripts set
# ENV_FILE before calling env_value.
env_value() {
  [ -f "${ENV_FILE:-}" ] || return 0
  awk -F= -v key="$1" '
    $0 !~ /^[[:space:]]*(#|$)/ && $1 == key {
      value = substr($0, length($1) + 2)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
      gsub(/^"|"$/, "", value)
      gsub(/^'\''|'\''$/, "", value)
      print value
    }
  ' "$ENV_FILE" | tail -n 1
}
