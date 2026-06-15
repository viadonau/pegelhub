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
- `INFLUX_LATEST_RANGE` (optional, defaults to `72h`)

`InfluxDBConfiguration` creates one shared client from `INFLUX_URL`, `INFLUX_ORG`, and `INFLUX_TOKEN`.
Repository operations pass the target bucket explicitly, so the data and telemetry buckets do not need separate client instances.
The actuator Influx health check pings the server and performs a tiny read query against both configured buckets.
Latest-value endpoints use `INFLUX_LATEST_RANGE` to define how far back they search.

In local Compose, the same declared token is passed both to InfluxDB first-start setup and to `core-app`. In production, the app should use a dedicated least-privilege token instead of an admin token.

## Application Model

The core application treats InfluxDB as one server connection with multiple buckets:

- `InfluxProperties` binds and validates `pegelhub.influx.*`.
- `InfluxDBConfiguration` creates one shared `InfluxDBClient`.
- `DatabaseProperties` describes a concrete bucket plus the shared organization/token.
- Repositories pass bucket and organization explicitly for every write and query.
- `ConnectionHelper` is the only place that translates Flux `FluxTable`/`FluxRecord` rows into PegelHub's internal `InfluxPoint` representation.

This avoids one client per bucket and keeps the bucket choice visible at each repository boundary.

## Query Model

Flux query strings are built through `FluxQueries`, not directly in repositories.
All relative user-facing ranges are parsed as `FluxDuration` before a query is built.

Accepted durations are positive Flux durations such as:

- `5m`
- `72h`
- `7d`
- `1h30m`

Invalid ranges fail before the query reaches InfluxDB. This keeps syntax errors and query injection attempts out of the Flux layer.

Latest endpoints use the configured `INFLUX_LATEST_RANGE` instead of an embedded repository constant. If a valid station has older data than that range, latest endpoints intentionally report no latest value in the configured search window.

## Time Handling

Core API and domain timestamps for measurement and telemetry values are absolute UTC instants:

```json
{
  "timestamp": "2026-04-25T10:15:30Z"
}
```

Offset-free timestamps such as `2026-04-25T10:15:30` are no longer accepted by the core API for Influx-backed values.
This is intentional because offset-free local date-times cannot be safely written to a shared time-series database without guessing a timezone.

The conversion rules are:

- Incoming measurement write DTOs use `Instant`.
- Incoming telemetry payloads use `Instant`.
- Influx writes pass `Instant` directly to the Java client with millisecond precision.
- Influx reads expose record `_time` directly as `Instant`.
- Aggregate measurement queries without `_time`, such as averages, receive the current UTC `Instant` as their response timestamp.
- `/api/v1/measurement/systemTime` returns the Influx server time as an `Instant`.

Connector-library measurement and telemetry models also use `Instant`.
Connectors that parse offset-free protocol timestamps convert them to UTC at the protocol boundary before sending data to Core.
The connector library expects HTTP timestamp values to be ISO-8601 instant strings and writes outbound connector payloads in that same format.

## Manual Dev Profile

For a non-container app run, start local dependencies through the helper, then run `core-app` with the `dev` Spring profile:

```bash
bash .agents/skills/pegelhub-local-dev/scripts/pegelhub-local-dev.sh compose-up-deps
```

The `dev` Spring profile defaults to:

```yaml
pegelhub:
  influx:
    url: ${INFLUX_URL:http://localhost:8111/}
    org: ${INFLUX_ORG:pegelhub}
    token: ${INFLUX_TOKEN:local-dev-influx-token-change-me-000000000000000000000000000000}
    data-bucket: ${INFLUX_DATA_BUCKET:pegelhub-data}
    telemetry-bucket: ${INFLUX_TELEMETRY_BUCKET:pegelhub-telemetry}
    latest-range: ${INFLUX_LATEST_RANGE:72h}
```

Override those variables if your local InfluxDB uses different values.

## Previous Setup

The old flow used:

- `testenvironment.vars`
- `init-influxdb.sh`
- `.datastoreconfig/storeapp.yaml`
- `INFLUX_FILE`

That setup generated a token at container startup, wrote it to a mounted YAML file, and made the app read that file. The current setup removes that generated-file handoff and uses normal environment-based Spring configuration instead.
