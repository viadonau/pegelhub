# Pegelhub Core

`pegelhub-core` is a Spring Boot monolith built around one deployable module, `core-app`.

## Modules

- `core-app`: the application, including HTTP endpoints, business logic, JDBC persistence, Influx persistence, actuator, runtime configuration, and integration test support.

The application is exposed directly on:

- `http://localhost:8080` for the public API under `/api/v1/**`
- `http://localhost:8081/actuator` for management endpoints

Legacy proxy compatibility routes are intentionally removed.

## Requirements

- Java 17
- Maven 3.8+
- Docker

## Build

From `pegelhub-core/`:

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

Run:

```bash
cp .env.example .env
docker compose up --build -d
```

The app is then reachable on `localhost:8080` and actuator on `localhost:8081`.

## API Client Docs

The Postman collection for the core HTTP API lives in `docs/api/postman/`.

## Manual Dev Profile

For a non-container app run, start local Postgres and InfluxDB, then run the app with the `dev` profile:

```bash
docker run --name postgres -p 5432:5432 \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=pegelhub \
  -d postgres

docker run --name influxdb -p 8086:8086 \
  -e DOCKER_INFLUXDB_INIT_MODE=setup \
  -e DOCKER_INFLUXDB_INIT_USERNAME=admin \
  -e DOCKER_INFLUXDB_INIT_PASSWORD=admin1234 \
  -e DOCKER_INFLUXDB_INIT_ORG=pegelhub \
  -e DOCKER_INFLUXDB_INIT_BUCKET=pegelhub-internal \
  -e DOCKER_INFLUXDB_INIT_ADMIN_TOKEN=local-dev-influx-token-change-me-000000000000000000000000000000 \
  -e INFLUX_DATA_BUCKET=pegelhub-data \
  -e INFLUX_TELEMETRY_BUCKET=pegelhub-telemetry \
  -v "$PWD/docker/influxdb/init:/docker-entrypoint-initdb.d:ro" \
  -d influxdb:2.2-alpine
```

The `dev` profile defaults to the token shown above. Override `INFLUX_TOKEN`, `INFLUX_ORG`, `INFLUX_DATA_BUCKET`, and `INFLUX_TELEMETRY_BUCKET` when you use different local values.

## Packaging

Build the application jar and Docker image from `core-app`:

```bash
mvn -DskipTests package
docker build ./core-app -t pegelhub-core-app:latest
```

`docker-compose.yaml`, `.env.example`, and `docker/influxdb/init/` are intended as a local or self-hosted developer setup. Runtime secrets should be injected through environment variables or a deployment secret manager, not committed to the repository.
