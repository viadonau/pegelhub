# Pegelhub Core

Pegelhub Core is the Spring Boot monolith for Pegelhub.

The application is exposed directly on:

- `http://localhost:8080` for the public API under `/api/v1/**`
- `http://localhost:8081/actuator` for management endpoints

Legacy proxy compatibility routes are intentionally removed.

## Requirements

- Java 21
- Maven 3.8+
- Docker

## Build

From `core/`:

```bash
mvn test
mvn -DskipTests package
```

## Local Development

### IntelliJ

Shared IntelliJ run configurations live in the repository-level `.run/` directory.

- `Core: Local Development`: starts Core from the IDE with the `dev` Spring profile.
- `Image: FTP Connector`: builds `pegelhub-ftp-connector:local`.
- `Image: ICC Connector`: builds `pegelhub-icc-connector:local`.
- `Image: IEC Connector`: builds `pegelhub-iec-connector:local`.
- `Image: mA Connector`: builds `pegelhub-ma-connector:local`.
- `Image: TSTP Connector`: builds `pegelhub-tstp-connector:local`.
- `Tests: Core`: runs Core unit tests.
- `Tests: Connectors`: runs connector unit tests.

### Docker Compose

The local compose setup starts:

- `core-app`
- `meta-db` (Postgres)
- `data-db` (InfluxDB)
- `keycloak-db` (Postgres for local identity)
- `keycloak`

Run from the repository root:

```bash
test -f core/.env || cp core/.env.example core/.env
scripts/local-stack.sh compose-up
```

The app is then reachable on `localhost:8080`, actuator on `localhost:8081`, and local Keycloak on `http://pegelhub-keycloak.test:8082`.

The token in `.env` is the source of truth for local first-start setup. If your local InfluxDB volume was already initialized with a different token, update `.env` to match it or recreate the local InfluxDB volume intentionally.

InfluxDB setup, environment variables, and migration notes from the old generated-token flow are documented in `docs/influxdb.md`.

Core owns its relational metadata schema with Flyway migrations in `src/main/resources/db/migration/`. Hibernate validates the schema on startup and no longer creates or updates tables. Keep `FLYWAY_BASELINE_ON_MIGRATE=false` for fresh databases and normal operation. Follow the [Flyway rollout procedure](docs/flyway.md) before the one-time baseline of an existing Hibernate-created schema.

Useful local stack commands from the repository root:

```bash
scripts/local-stack.sh status
scripts/local-stack.sh logs core-app
scripts/local-stack.sh smoke
scripts/local-stack.sh compose-down
```

Keycloak setup and auth operations are documented in:

- `docs/keycloak-local-dev.md`
- `docs/keycloak-operations.md`

## API Client Docs

OpenAPI documentation is served by the running Core app:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- OpenAPI YAML: `http://localhost:8080/v3/api-docs.yaml`

Swagger UI is public, but protected API calls still require a Keycloak access token. Use the Authorize button with a Bearer JWT for the required PegelHub role, such as `METADATA_READ`, `METADATA_WRITE`, `MEASUREMENT_READ`, `MEASUREMENT_WRITE`, `TELEMETRY_READ`, `TELEMETRY_WRITE`, or `SYSTEM_ADMIN`.

The legacy Postman collection for the core HTTP API lives in `docs/api/postman/` and also uses Bearer tokens from Keycloak.

## Manual Dev Profile

For a non-container app run, start local dependencies from `core/`, then run the app with the `dev` profile:

```bash
test -f .env || cp .env.example .env
docker compose --env-file .env up -d meta-db data-db keycloak-db keycloak
```

The `dev` profile defaults to the local dependency ports:

- Postgres: `localhost:5444`
- InfluxDB: `http://localhost:8111/`
- Keycloak issuer: `http://pegelhub-keycloak.test:8082/realms/pegelhub`

Add this hosts entry before running Core from the host:

```text
127.0.0.1 pegelhub-keycloak.test
```

Override `KEYCLOAK_ISSUER_URI`, `PEGELHUB_FRONTEND_URL`, `INFLUX_TOKEN`, `INFLUX_ORG`, `INFLUX_DATA_BUCKET`, and `INFLUX_TELEMETRY_BUCKET` when you use different local values.

## Packaging

Build the application jar from `core/`:

```bash
mvn -DskipTests package
```

Build the Docker image from `core/`. The Dockerfile builds the jar inside the image build, so it does not require a pre-existing local `target/app.jar`:

```bash
docker build . -t pegelhub-core:latest
```

`docker-compose.yaml`, `.env.example`, and `docker/influxdb/init/` are intended as a local or self-hosted developer setup. Runtime secrets should be injected through environment variables or a deployment secret manager, not committed to the repository.
