#!/usr/bin/env sh

set -eu

fail() {
  printf 'InfluxDB bucket configuration failed: %s\n' "$*" >&2
  exit 1
}

require_value() {
  variable_name="$1"
  variable_value="$2"

  [ -n "$variable_value" ] || fail "$variable_name is required."
}

validate_retention() {
  variable_name="$1"
  retention="$2"

  if [ "$retention" = "0s" ]; then
    return
  fi

  printf '%s\n' "$retention" | grep -Eq '^[1-9][0-9]*(h|d|w)$' \
    || fail "$variable_name must be 0s or a positive whole number of hours, days, or weeks."
}

configure_bucket() {
  bucket_name="$1"
  retention="$2"
  bucket_list=$(influx bucket list --hide-headers) \
    || fail "Could not list InfluxDB buckets."
  bucket_id=$(printf '%s\n' "$bucket_list" \
    | awk -v bucket_name="$bucket_name" '$2 == bucket_name { print $1; exit }')

  if [ -z "$bucket_id" ]; then
    influx bucket create --name "$bucket_name" --retention "$retention" >/dev/null
    printf 'Created InfluxDB bucket %s with retention %s.\n' "$bucket_name" "$retention"
    return
  fi

  influx bucket update --id "$bucket_id" --retention "$retention" >/dev/null
  printf 'Configured InfluxDB bucket %s with retention %s.\n' "$bucket_name" "$retention"
}

require_value INFLUX_HOST "${INFLUX_HOST:-}"
require_value INFLUX_ORG "${INFLUX_ORG:-}"
require_value INFLUX_TOKEN "${INFLUX_TOKEN:-}"
require_value INFLUX_INTERNAL_BUCKET "${INFLUX_INTERNAL_BUCKET:-}"
require_value INFLUX_DATA_BUCKET "${INFLUX_DATA_BUCKET:-}"
require_value INFLUX_DATA_RETENTION "${INFLUX_DATA_RETENTION:-}"
require_value INFLUX_TELEMETRY_BUCKET "${INFLUX_TELEMETRY_BUCKET:-}"
require_value INFLUX_TELEMETRY_RETENTION "${INFLUX_TELEMETRY_RETENTION:-}"

validate_retention INFLUX_DATA_RETENTION "$INFLUX_DATA_RETENTION"
validate_retention INFLUX_TELEMETRY_RETENTION "$INFLUX_TELEMETRY_RETENTION"

if [ "$INFLUX_INTERNAL_BUCKET" = "$INFLUX_DATA_BUCKET" ] \
  || [ "$INFLUX_INTERNAL_BUCKET" = "$INFLUX_TELEMETRY_BUCKET" ] \
  || [ "$INFLUX_DATA_BUCKET" = "$INFLUX_TELEMETRY_BUCKET" ]; then
  fail "INFLUX_INTERNAL_BUCKET, INFLUX_DATA_BUCKET, and INFLUX_TELEMETRY_BUCKET must be different."
fi

configure_bucket "$INFLUX_DATA_BUCKET" "$INFLUX_DATA_RETENTION"
configure_bucket "$INFLUX_TELEMETRY_BUCKET" "$INFLUX_TELEMETRY_RETENTION"
