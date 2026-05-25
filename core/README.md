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

- `Pegelhub Core: Local Development`: starts the app with the `dev` Spring profile.
- `Pegelhub Core: Docker Compose`: starts the databases plus app container through `docker-compose.yaml`.

### Docker Compose

The local compose setup starts:

- `core-app`
- `meta-db` (Postgres)
- `data-db` (InfluxDB)
- `keycloak-db` (Postgres for local identity)
- `keycloak`

Run:

```bash
bash ../.agents/skills/pegelhub-local-dev/scripts/pegelhub-local-dev.sh init-env
bash ../.agents/skills/pegelhub-local-dev/scripts/pegelhub-local-dev.sh compose-up
```

The app is then reachable on `localhost:8080`, actuator on `localhost:8081`, and local Keycloak on `http://pegelhub-keycloak.test:8082`.

The token in `.env` is the source of truth for local first-start setup. If your local InfluxDB volume was already initialized with a different token, update `.env` to match it or recreate the local InfluxDB volume intentionally.

InfluxDB setup, environment variables, and migration notes from the old generated-token flow are documented in `docs/influxdb.md`.

Keycloak setup and auth operations are documented in:

- `docs/keycloak-local-dev.md`
- `docs/keycloak-operations.md`

## API Client Docs

The Postman collection for the core HTTP API lives in `docs/api/postman/` and uses Bearer tokens from Keycloak.

## Manual Dev Profile

For a non-container app run, start local dependencies through the helper, then run the app with the `dev` profile:

```bash
bash ../.agents/skills/pegelhub-local-dev/scripts/pegelhub-local-dev.sh init-env
bash ../.agents/skills/pegelhub-local-dev/scripts/pegelhub-local-dev.sh compose-up-deps
```

The `dev` profile defaults to the helper's local dependency ports:

- Postgres: `localhost:5444`
- InfluxDB: `http://localhost:8111/`
- Keycloak issuer: `http://pegelhub-keycloak.test:8082/realms/pegelhub`

Add this hosts entry before running Core from the host:

```text
127.0.0.1 pegelhub-keycloak.test
```

Override `KEYCLOAK_ISSUER_URI`, `INFLUX_TOKEN`, `INFLUX_ORG`, `INFLUX_DATA_BUCKET`, and `INFLUX_TELEMETRY_BUCKET` when you use different local values.

## Packaging

Build the application jar from `core/`:

```bash
mvn -DskipTests package
```

Build the Docker image from `core/`. The Dockerfile builds the jar inside the image build, so it does not require a pre-existing local `target/app.jar`:

```bash
docker build . -t pegelhub-core:latest
```

## Release Runtime

Published Core images can be run with `docker-compose.release.yaml` and
`.env.release.example`. The release deployment flow, image pinning, backups, rollback,
and connector runtime notes are documented in `docs/release-deployment.md`.

`docker-compose.yaml`, `.env.example`, and `docker/influxdb/init/` are intended as a local or self-hosted developer setup. Runtime secrets should be injected through environment variables or a deployment secret manager, not committed to the repository.
