# PegelHub Core

Core is the Java 21 / Spring Boot 4 backend for PegelHub. It provides the HTTP
API used by the [monitoring frontend](../frontend/README.md), connectors, and
operators. PostgreSQL stores metadata; separate InfluxDB buckets store
measurements and connector technical telemetry. Keycloak issues the bearer
tokens used to access protected routes.

Start with the [repository README](../README.md) for the full system. This guide
covers Core development, configuration, API access, and verification.

## Responsibilities

| Area | Core provides |
| --- | --- |
| Metadata | Creation, listing, lookup, and updates for station owners, stations, measuring points, time series, and connectors |
| Measurement catalog | Supported observed properties, canonical units, and input/output representations through `/api/v1/observed-properties` |
| Measurements | Connector-authenticated ingestion, bounded raw reads, and bucketed chart reads |
| Monitoring | Operator read models combining metadata and latest measurements at `/api/v1/monitoring/time-series` |
| Connector access | Keycloak client identity registration and explicit station/time-series read grants |
| Telemetry | Ingestion and queries for connector diagnostics, separately from hydrological measurements |

The metadata hierarchy is `StationOwner -> Station -> MeasuringPoint ->
TimeSeries`. A time series identifies an observed property and may assign one
source connector and input representation. Core currently supports water level
(`cm`), water temperature (`Cel`), and discharge (`m3/s`) as canonical storage
units. The catalog also advertises `metres-above-adria` for water level and
`litres-per-second` for discharge. Metadata administration is an API capability,
not a frontend administration screen.

## Requirements

- Java 21 and Maven 3.9 for host builds, matching the repository's container builds
- Docker Engine with Docker Compose v2 for the local stack and integration tests
- Bash and `curl` for the local-stack helper

Java and Maven do not need to be installed on the host for a Docker-only run.
Commands below run from the repository root unless noted otherwise.

## Local Docker stack

Create the ignored environment file and start all local services:

```bash
test -f core/.env || cp core/.env.example core/.env
scripts/local-stack.sh compose-up
scripts/local-stack.sh health
```

`compose-up` validates the Compose model, builds Core, starts the dependencies,
and waits for actuator health. The stack contains `core-app`, PostgreSQL
`meta-db`, InfluxDB `data-db`, the one-shot `influx-bucket-setup`, Keycloak, and
Keycloak's PostgreSQL database.

| Service | Local address |
| --- | --- |
| Core API | `http://localhost:8080/api/v1` |
| Actuator | `http://localhost:8081/actuator` |
| Keycloak | `http://pegelhub-keycloak.test:8082` |
| PostgreSQL | `localhost:5444` |
| InfluxDB | `http://localhost:8111` |

The stack does not start the Angular frontend or connector processes. A new
metadata database is empty; use the [Bruno workflow](docs/api/bruno/README.md#write-workflow)
to create a small example dataset or provision metadata for your connectors.

Add this host entry when using Keycloak from the host or a browser:

```text
127.0.0.1 pegelhub-keycloak.test
```

Common stack operations:

```bash
scripts/local-stack.sh status
scripts/local-stack.sh compose-ps
scripts/local-stack.sh logs core-app
scripts/local-stack.sh logs-errors core-app
scripts/local-stack.sh restart core-app
scripts/local-stack.sh compose-down
```

`compose-down` preserves named volumes. The InfluxDB token in `core/.env` is
used both for first initialization and by Core. If an existing volume was
initialized with another token, make the file match that token or deliberately
recreate the local data volume. See [InfluxDB configuration](docs/influxdb.md).

## Host application run

To run Core in the IDE or with Maven while its dependencies remain in Docker,
stop any containerized `core-app` first so ports 8080 and 8081 are free:

```bash
test -f core/.env || cp core/.env.example core/.env
docker compose --env-file core/.env -f core/docker-compose.yaml \
  stop core-app
docker compose --env-file core/.env -f core/docker-compose.yaml \
  up -d meta-db data-db influx-bucket-setup keycloak-db keycloak
```

Then start `at.pegelhub.CoreAppApplication` with the `dev` Spring profile. The
shared IntelliJ configuration `Core: Local Development` in [`.run/`](../.run/)
selects that profile and uses `core/` as its working directory. The development
profile defaults to the host ports above. `core/.env` configures Compose; Maven
and the IDE do not load it automatically. If those values differ from the
development profile, supply matching Spring configuration or environment
overrides to the host process. Database overrides use
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and
`SPRING_DATASOURCE_PASSWORD`; the InfluxDB and Keycloak variables are listed below.

The equivalent Maven run is:

```bash
SPRING_PROFILES_ACTIVE=dev mvn -B -ntp -f core/pom.xml spring-boot:run
```

## Build and test

```bash
mvn -B -ntp -f core/pom.xml test
mvn -B -ntp -f core/pom.xml -DskipTests package
```

The full Core verification, including tests tagged `integration-test`, is:

```bash
mvn -B -ntp -f core/pom.xml -Pintegration verify
```

Integration tests use Testcontainers and need Docker. The repository-wide CI
command is `mvn -B -ntp -Pintegration verify` from the root.
The packaged application is `core/target/app.jar`.

Build the Core container without first building a host JAR:

```bash
docker build -f core/Dockerfile -t pegelhub-core:local .
```

## Configuration

The containerized application accepts these main values; the local Compose file
derives them from `core/.env`:

| Variable | Purpose |
| --- | --- |
| `DB_URI`, `DB_USER`, `DB_PASSWORD` | PostgreSQL connection |
| `KEYCLOAK_ISSUER_URI` | Exact issuer accepted for JWT validation |
| `INFLUX_URL`, `INFLUX_ORG`, `INFLUX_TOKEN` | InfluxDB connection |
| `INFLUX_DATA_BUCKET` | Measurement bucket |
| `INFLUX_TELEMETRY_BUCKET` | Technical telemetry bucket |
| `INFLUX_LATEST_RANGE` | Default latest-telemetry search range; defaults to `72h` |

Bucket retention is reconciled by the Compose setup service with
`INFLUX_DATA_RETENTION` and `INFLUX_TELEMETRY_RETENTION`. Read
[the InfluxDB guide](docs/influxdb.md) before changing retention.

## Database migrations

Flyway owns the PostgreSQL metadata schema through the ordered migrations in
[`src/main/resources/db/migration/`](src/main/resources/db/migration/).
`V1__initial_metadata_schema.sql` creates the current metadata model;
`V2__litres_per_second_representation.sql` extends the discharge representation
constraint. Hibernate uses `ddl-auto: validate` and does not create or update
the schema.

The current metadata model replaced the legacy schema with a clean baseline.
Do not apply that baseline to a data-bearing legacy database without a migration
plan. Measurements refer to persisted TimeSeries UUIDs, so a deliberate metadata
reset must also account for the associated InfluxDB data. Routine application
upgrades apply pending Flyway migrations; they do not require resetting volumes.
See the [Flyway guide](docs/flyway.md) for the legacy reset boundary.

## OpenAPI and API examples

With Core running:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- JSON: `/v3/api-docs?lang=en` or `/v3/api-docs?lang=de`
- YAML: `/v3/api-docs.yaml?lang=en` or `/v3/api-docs.yaml?lang=de`

Requests without `lang` produce deterministic English for machine consumers.
Swagger UI opens the German definition by default and offers both languages.
The generated OpenAPI document is the authoritative HTTP contract.

The repository-owned [Bruno collection](docs/api/bruno/README.md) provides
runnable local and remote examples. Requests tagged `read-only` work against an
empty database. Other requests may require existing IDs or deliberately change
persistent metadata, measurements, or telemetry.

Monitoring is a separate read contract from metadata administration. Its
collection includes only active Station -> MeasuringPoint -> TimeSeries paths;
detail responses retain access to inactive series and report their effective
status. Both accept `latestWithin` (default and maximum `365d`), which bounds
the latest-measurement lookup rather than guaranteeing a recent reading.

## Security model

Core is a stateless OAuth 2.0 resource server. It validates the configured
Keycloak issuer and requires the `pegelhub-core-api` audience. Authorities are
read only from that client's roles in the JWT `resource_access` claim.

The runtime role values are:

| Role | Scope |
| --- | --- |
| `metadata:read` | Read metadata as a `USER` actor |
| `metadata:write` | Read and write metadata as a `USER` actor on supported routes |
| `measurement:read` | Read measurements; `CLIENT` actors also need an active connector and an explicit read grant |
| `measurement:write` | Submit measurements as an active, registered `CLIENT` connector with source ownership |
| `telemetry:read` | Read technical telemetry as a `USER` actor |
| `telemetry:write` | Submit technical telemetry as an active, registered `CLIENT` connector |
| `system:admin` | Register connector identities as a `USER`, access protected actuator routes, and use the explicit route-specific fallbacks |

Tokens identify the actor through `pegelhub_actor_type` (`USER` or `CLIENT`).
Monitoring reads require a `USER` actor with both `metadata:read` and
`measurement:read`, or a `USER` with `system:admin`. Metadata and telemetry reads
are not available to connector actors, even if they carry the corresponding
read role.

Connector measurement access also uses `pegelhub_actor_type`, the token client
ID, active Connector metadata, time-series source ownership, and explicit read
access relations. The [connector library guide](../connectors/library/#core-authorization-prerequisites)
summarizes those prerequisites.

`system:admin` is not a universal authorization bypass. In particular,
`POST /api/v1/measurements` still requires `measurement:write`, a `CLIENT`
actor, an active registered Connector, exact source ownership, and an active
Station -> MeasuringPoint -> TimeSeries path. Measurement-read bypass applies
only to `USER` administrators. Telemetry writes permitted by `system:admin`
still require a `CLIENT` actor and resolve an active Connector from the token
client ID. A source assignment permits writes; it does not grant connector
read access.

Swagger UI, OpenAPI documents, the configured actuator health/info surface, and
`/api/v1/measurements/system-time` are public. API authorization is enforced per
route and HTTP method. Use the lowercase role values above when configuring
Keycloak. Local realm details live in
[the local Keycloak guide](docs/keycloak-local-dev.md); staging identity
operations live in the [single-host deployment guide](../deploy/single-host/).

## Troubleshooting

- **Keycloak hostname does not resolve:** add the documented hosts entry. Keep
  the issuer URL identical between Keycloak, Core, and the token.
- **Core fails schema validation:** inspect Flyway history and follow the
  [rollout guide](docs/flyway.md); do not switch Hibernate to schema creation.
- **InfluxDB returns unauthorized:** ensure `core/.env` matches the token that
  initialized the existing volume.
- **A Compose service is unhealthy:** run `scripts/local-stack.sh compose-ps`
  and `scripts/local-stack.sh logs <service>`.
- **A local port is occupied:** stop the conflicting process or change the
  Compose port mapping and corresponding application URL consistently.

## Deployment boundary

`docker-compose.yaml`, `.env.example`, and `docker/` are for local development.
The currently supported remote workflow is staging, documented in
[`deploy/single-host/`](../deploy/single-host/). Runtime secrets belong in ignored host
configuration or a secret manager, never in this repository.
