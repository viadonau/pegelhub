#!/usr/bin/env sh

set -eu

create_bucket() {
  bucket_name="$1"

  if influx bucket list --org "$DOCKER_INFLUXDB_INIT_ORG" --name "$bucket_name" | grep -q "$bucket_name"; then
    echo "InfluxDB bucket already exists: $bucket_name"
    return
  fi

  influx bucket create --org "$DOCKER_INFLUXDB_INIT_ORG" --name "$bucket_name"
}

create_bucket "$INFLUX_DATA_BUCKET"
create_bucket "$INFLUX_TELEMETRY_BUCKET"
