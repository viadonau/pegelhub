# InfluxDB configuration

PegelHub stores time-series values in two InfluxDB buckets in one organization:

- `INFLUX_DATA_BUCKET` stores measurements and uses
  `INFLUX_DATA_RETENTION`.
- `INFLUX_TELEMETRY_BUCKET` stores technical telemetry and uses
  `INFLUX_TELEMETRY_RETENTION`.

Both retention settings default to `60d`. Use `0s` for infinite retention.

## Local stack

From the repository root:

```bash
test -f core/.env || cp core/.env.example core/.env
scripts/local-stack.sh compose-up
```

The stack exposes InfluxDB at <http://localhost:8111>. The one-shot
`influx-bucket-setup` service waits for InfluxDB, creates missing application
buckets, and updates existing buckets to the requested retention before Core
starts.

The provisioner accepts `0s` or a positive whole number of hours, days, or
weeks, such as `24h`, `60d`, or `8w`. The internal, measurement, and telemetry
bucket names must all be different.

Reducing finite retention, or changing from infinite to finite retention, can
permanently remove older points. InfluxDB applies expiry asynchronously;
restoring the previous setting does not restore deleted data.

`core/.env` is the source of truth for the local `INFLUX_TOKEN`. An existing
InfluxDB volume retains the token used at first initialization. If that token
differs from the environment file, either make the file match the existing
volume or deliberately recreate the local data volume.

## Runtime configuration

Core reads:

| Variable | Purpose |
| --- | --- |
| `INFLUX_URL` | InfluxDB HTTP endpoint |
| `INFLUX_ORG` | Organization containing the buckets |
| `INFLUX_TOKEN` | Token used by the shared client |
| `INFLUX_DATA_BUCKET` | Measurement bucket |
| `INFLUX_TELEMETRY_BUCKET` | Technical telemetry bucket |
| `INFLUX_LATEST_RANGE` | Latest-telemetry search horizon; defaults to `72h` |

Compose provisioning additionally reads `INFLUX_INTERNAL_BUCKET`,
`INFLUX_DATA_RETENTION`, and `INFLUX_TELEMETRY_RETENTION`.
`INFLUX_INTERNAL_BUCKET` is the first-start bucket required by the official
InfluxDB image; Core does not use it.

Both repository Compose topologies currently pass `INFLUX_TOKEN` as the
InfluxDB first-start admin token and as Core's runtime token. They do not
provision a separate least-privilege runtime token. Treat that token as an
administrative secret and do not expose InfluxDB publicly.

## Application and query model

`InfluxProperties` binds and validates `pegelhub.influx.*`.
`InfluxDBConfiguration` creates one shared `InfluxDBClient` and two
bucket-specific `InfluxBucketOperations` objects. Measurement and telemetry
repositories use their respective bucket operations, query builders, point
mappers, and row mappers.

Relative API ranges and `INFLUX_LATEST_RANGE` are validated as
`PegelhubDurationLiteral` values before Flux is built. They consist of one or
more positive integer parts with `s`, `m`, `h`, `d`, or `w`, for example `5m`,
`72h`, `7d`, or `1h30m`. Measurement and telemetry repositories construct Flux
through `MeasurementFluxQueryBuilder` and `TelemetryFluxQueryBuilder`.

Only the latest-telemetry query uses `INFLUX_LATEST_RANGE`. Raw and bucketed
measurement APIs use an explicit `last` duration or `from`/`to` window supplied
by the caller. The connector library's latest-measurement helper independently
uses a fixed 365-day query window.

The actuator Influx health indicator pings the server and validates that both
application buckets can be queried.

## Time handling

Core measurement and telemetry timestamps are absolute `Instant` values. API
payloads therefore require an offset, normally UTC `Z`, for example:

```json
{
  "observedAt": "2026-04-25T10:15:30Z"
}
```

Offset-free timestamps such as `2026-04-25T10:15:30` are rejected. Influx
writes pass `Instant` values to the Java client with millisecond precision, and
reads map Influx `_time` values back to `Instant`. Timestamps in bucketed query
results come from Flux aggregate-window boundaries. The public
`/api/v1/measurements/system-time` route returns the InfluxDB system time.

The connector library serializes measurement `Instant` values as ISO-8601.
Protocol parsers still own the conversion of offset-free source timestamps;
those semantics vary by connector and must be verified in its guide or source.

## Host application run

To run Core on the host while its dependencies stay in Docker:

```bash
test -f core/.env || cp core/.env.example core/.env
docker compose --env-file core/.env -f core/docker-compose.yaml \
  up -d meta-db data-db influx-bucket-setup keycloak-db keycloak
SPRING_PROFILES_ACTIVE=dev mvn -B -ntp -f core/pom.xml spring-boot:run
```

The `dev` profile defaults match `core/.env.example`. If values in `core/.env`
change, export matching application environment variables before starting
Maven.

To reconcile bucket retention explicitly after editing `core/.env`:

```bash
docker compose --env-file core/.env -f core/docker-compose.yaml \
  up --force-recreate influx-bucket-setup
docker compose --env-file core/.env -f core/docker-compose.yaml up -d core-app
```

## Provisioning test

The Docker-backed integration test verifies bucket creation, finite and
infinite retention updates, repeated execution, and invalid configuration:

```bash
mvn -B -ntp -f core/pom.xml -Pintegration \
  -Dtest=NoUnitTests \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dit.test=InfluxBucketProvisioningIntegrationTest verify
```
