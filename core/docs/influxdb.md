# InfluxDB Configuration

Pegelhub stores time-series data in InfluxDB and uses two buckets in one organization:

- `INFLUX_DATA_BUCKET` for measurement data
- `INFLUX_TELEMETRY_BUCKET` for telemetry data

The application does not read a generated token file anymore. Runtime configuration comes from environment variables through the `pegelhub.influx.*` Spring properties.

## Local Docker Compose

From `core/`:

```bash
cp .env.example .env
docker compose up --build -d
```

For local first-start setup, `.env` is the source of truth for `INFLUX_TOKEN`. If the InfluxDB volume already exists and was initialized with a different token, either update `.env` to match that existing token or recreate the local volume intentionally.

The compose setup starts:

- `meta-db` on host port `5444`
- `data-db` on host port `8111`
- `core-app` on `8080` and actuator on `8081`

`docker/influxdb/init/01-create-buckets.sh` only creates buckets during InfluxDB initialization. It does not generate app config files.

## Runtime Config

The app expects these variables:

- `INFLUX_URL`
- `INFLUX_ORG`
- `INFLUX_TOKEN`
- `INFLUX_DATA_BUCKET`
- `INFLUX_TELEMETRY_BUCKET`

In local Compose, the same declared token is passed both to InfluxDB first-start setup and to `core-app`. In production, the app should use a dedicated least-privilege token instead of an admin token.

## Manual Dev Profile

The `dev` Spring profile defaults to:

```yaml
pegelhub:
  influx:
    url: ${INFLUX_URL:http://localhost:8086/}
    org: ${INFLUX_ORG:pegelhub}
    token: ${INFLUX_TOKEN:local-dev-influx-token-change-me-000000000000000000000000000000}
    data-bucket: ${INFLUX_DATA_BUCKET:pegelhub-data}
    telemetry-bucket: ${INFLUX_TELEMETRY_BUCKET:pegelhub-telemetry}
```

Override those variables if your local InfluxDB uses different values.

## Previous Setup

The old flow used:

- `testenvironment.vars`
- `init-influxdb.sh`
- `.datastoreconfig/storeapp.yaml`
- `INFLUX_FILE`

That setup generated a token at container startup, wrote it to a mounted YAML file, and made the app read that file. The current setup removes that generated-file handoff and uses normal environment-based Spring configuration instead.
