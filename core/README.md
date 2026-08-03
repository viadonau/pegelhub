# PegelHub Core

Core is the Spring Boot application behind the PegelHub HTTP API. It stores
metadata in PostgreSQL, stores measurements and technical telemetry in separate
InfluxDB buckets, authenticates bearer tokens issued by Keycloak, and exposes a
bilingual OpenAPI description.

## Requirements

- Java 21 and Maven 3.8 or newer for host builds
- Docker Engine with Docker Compose v2 for the local stack and integration tests
- `curl` for helper-script health checks

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

| Service | Host address |
| --- | --- |
| Core API | `http://localhost:8080/api/v1` |
| Actuator | `http://localhost:8081/actuator` |
| Keycloak | `http://pegelhub-keycloak.test:8082` |
| PostgreSQL | `localhost:5444` |
| InfluxDB | `http://localhost:8111` |

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

To run Core in the IDE or with Maven while its dependencies remain in Docker:

```bash
test -f core/.env || cp core/.env.example core/.env
docker compose --env-file core/.env -f core/docker-compose.yaml \
  up -d meta-db data-db influx-bucket-setup keycloak-db keycloak
```

Then start `at.pegelhub.CoreAppApplication` with the `dev` Spring profile. The
shared IntelliJ configuration `Core: Local Development` in [`.run/`](../.run/)
selects that profile and uses `core/` as its working directory. The development
profile defaults to the host ports above; add environment overrides to the run
configuration only when the local values differ.

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

Build the Core container without first building a host JAR:

```bash
docker build -t pegelhub-core:local core
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
| `FLYWAY_BASELINE_ON_MIGRATE` | One-time legacy schema baseline switch; normally `false` |

Bucket retention is reconciled by the Compose setup service with
`INFLUX_DATA_RETENTION` and `INFLUX_TELEMETRY_RETENTION`. Read
[the InfluxDB guide](docs/influxdb.md) before changing retention.

## Database migrations

Flyway owns the PostgreSQL metadata schema through migrations in
[`src/main/resources/db/migration/`](src/main/resources/db/migration/).
Hibernate runs with `ddl-auto: validate`; it does not create or update the
schema. Fresh databases and normal operation use
`FLYWAY_BASELINE_ON_MIGRATE=false`.

An existing schema previously created by Hibernate needs the documented,
backup-first [Flyway rollout procedure](docs/flyway.md). Do not enable the
baseline setting as a general startup workaround.

## OpenAPI and API examples

With Core running:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- JSON: `/v3/api-docs?lang=en` or `/v3/api-docs?lang=de`
- YAML: `/v3/api-docs.yaml?lang=en` or `/v3/api-docs.yaml?lang=de`

Requests without `lang` produce deterministic English for machine consumers.
Swagger UI opens the German definition by default and offers both languages.
The generated OpenAPI document is the authoritative HTTP contract.

The repository-owned [Bruno collection](docs/api/bruno/) provides runnable
local and remote examples. Its read-only requests can be executed against an
empty database; its workflow requests deliberately create persistent metadata
and measurements.

## Security model

Core is a stateless OAuth 2.0 resource server. It validates the configured
Keycloak issuer and requires the `pegelhub-core-api` audience. Authorities are
read only from that client's roles in the JWT `resource_access` claim.

The runtime role values are:

| Role | Scope |
| --- | --- |
| `metadata:read` | Read metadata resources |
| `metadata:write` | Create, update, or delete metadata resources |
| `measurement:read` | Read time-series measurements |
| `measurement:write` | Submit measurements as an authenticated connector |
| `telemetry:read` | Read technical telemetry |
| `telemetry:write` | Submit technical telemetry |
| `system:admin` | Connector registration, protected management access, and explicit admin fallbacks on API reads/writes |

Connector measurement access also uses `pegelhub_actor_type`, the token client
ID, active Connector metadata, time-series source ownership, and access grants.
The [connector library guide](../connectors/library/#core-authorization-prerequisites)
summarizes those prerequisites.

Swagger UI, OpenAPI documents, the configured actuator health/info surface, and
`/api/v1/measurements/system-time` are public. API authorization is enforced per
route and HTTP method. Use the lowercase role values above when configuring
Keycloak. Local realm details live in
[the local Keycloak guide](docs/keycloak-local-dev.md); staging identity
operations live in the [staging guide](../deploy/staging/#ftp-connector-configuration).

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
[`deploy/staging/`](../deploy/staging/). Runtime secrets belong in ignored host
configuration or a secret manager, never in this repository.
