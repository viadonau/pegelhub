# InfluxDB Configuration

Pegelhub stores time-series data in InfluxDB. The core app uses two buckets in the same InfluxDB organization:

- `INFLUX_DATA_BUCKET`: measurement data.
- `INFLUX_TELEMETRY_BUCKET`: telemetry data.

The application no longer reads Influx credentials from a generated `storeapp.yaml` file. Runtime configuration is now passed through Spring properties backed by environment variables.

## Current Model

The Spring config contract is:

```yaml
pegelhub:
  influx:
    url: ${INFLUX_URL}
    org: ${INFLUX_ORG}
    token: ${INFLUX_TOKEN}
    data-bucket: ${INFLUX_DATA_BUCKET}
    telemetry-bucket: ${INFLUX_TELEMETRY_BUCKET}
```

`InfluxDBConfiguration` creates one client for the data bucket and one client for the telemetry bucket. Repositories select the correct client and bucket through Spring qualifiers.

The local Docker setup uses one explicit token:

- InfluxDB receives it as `DOCKER_INFLUXDB_INIT_ADMIN_TOKEN`.
- `core-app` receives the same value as `INFLUX_TOKEN`.

This keeps provisioning and runtime config separate. InfluxDB creates credentials; the app only consumes credentials.

## Local Docker Compose

From `core/`:

```bash
cp .env.example .env
docker compose up --build -d
```

The compose setup starts:

- `meta-db`: Postgres metadata store on host port `5444`.
- `data-db`: InfluxDB on host port `8111`.
- `core-app`: API on `8080`, actuator on `8081`.

`docker/influxdb/init/01-create-buckets.sh` runs during first InfluxDB initialization and creates the measurement and telemetry buckets. It does not write app configuration files.

If the InfluxDB volume already exists, Docker will not rerun the InfluxDB first-start setup. Change `.env` values before the first start of a fresh volume, or recreate the volume intentionally when you need to reinitialize local InfluxDB.

## Environment Variables

Local defaults live in `.env.example`. Copy it to `.env` and adjust local-only values there.

| Variable | Used by | Meaning |
| --- | --- | --- |
| `INFLUX_URL` | `core-app` | InfluxDB base URL. Compose sets this to `http://data-db:8086/`; the dev profile defaults to `http://localhost:8086/`. |
| `INFLUX_ORG` | InfluxDB, `core-app` | Influx organization used by both buckets. |
| `INFLUX_TOKEN` | InfluxDB, `core-app` | Token used by the app to read and write buckets. Local dev uses a deterministic throwaway token. |
| `INFLUX_INTERNAL_BUCKET` | InfluxDB | Initial setup bucket required by the official InfluxDB image. The app does not use it. |
| `INFLUX_DATA_BUCKET` | InfluxDB, `core-app` | Bucket for measurement data. |
| `INFLUX_TELEMETRY_BUCKET` | InfluxDB, `core-app` | Bucket for telemetry data. |
| `INFLUX_ADMIN_USER` | InfluxDB | Local admin username for first-start setup. |
| `INFLUX_ADMIN_PASSWORD` | InfluxDB | Local admin password for first-start setup. |

Do not commit real tokens or passwords. Use `.env` for local values and deployment secrets for non-local environments.

## Manual Dev Profile

For a non-container app run, start Postgres and InfluxDB yourself, then run `core-app` with the `dev` Spring profile.

The `dev` profile has defaults for local InfluxDB:

```yaml
pegelhub:
  influx:
    url: ${INFLUX_URL:http://localhost:8086/}
    org: ${INFLUX_ORG:pegelhub}
    token: ${INFLUX_TOKEN:local-dev-influx-token-change-me-000000000000000000000000000000}
    data-bucket: ${INFLUX_DATA_BUCKET:pegelhub-data}
    telemetry-bucket: ${INFLUX_TELEMETRY_BUCKET:pegelhub-telemetry}
```

Override those environment variables if your manually started InfluxDB uses different values.

## Tests

Influx repository integration tests use Testcontainers through `PegelHubInfluxContainer`. They do not depend on local Docker Compose, `.env`, `.datastoreconfig`, or manual InfluxDB setup.

Run the regular test suite from `core/`:

```bash
mvn test
```

Run the Influx integration slice:

```bash
mvn -f core/pom.xml -Pintegration -Dtest=NoUnitTests -DfailIfNoTests=false -Dit.test=Influx* verify
```

## Previous Setup

The old setup used:

- `testenvironment.vars`
- `init-influxdb.sh`
- `.datastoreconfig/storeapp.yaml`
- `INFLUX_FILE`

That approach generated an Influx token at container startup, wrote it to a mounted YAML file, and made the app read that file as an alternate config source. This mixed infrastructure provisioning with application runtime config and risked committing generated tokens.

The replacement is:

- `.env.example` documents local env vars.
- `.env` stores local private overrides and is ignored by Git.
- `docker/influxdb/init/01-create-buckets.sh` only provisions buckets.
- `application.yaml` reads Influx runtime config from environment variables.
