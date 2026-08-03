# PegelHub backend

PegelHub is a backend for collecting water-level and related measurement data
from protocol-specific connectors. The repository contains the Spring Boot Core
API, shared connector runtime, five connector implementations, local development
services, and the supported staging deployment tooling.

This repository supports local development, pull-request CI, and deployment to
the staging topology described under [`deploy/staging/`](deploy/staging/). It
does not define or claim a production environment.

## Repository map

| Path | Responsibility |
| --- | --- |
| [`core/`](core/) | Core HTTP API, metadata in PostgreSQL, measurements and telemetry in InfluxDB, Flyway migrations, and local Docker stack |
| [`connectors/library/`](connectors/library/) | Shared Core client, YAML configuration helpers, mapping model, and connector lifecycle |
| [`connectors/ftp-connector/`](connectors/ftp-connector/) | Imports ASC or ZRXP files from FTP |
| [`connectors/icc-connector/`](connectors/icc-connector/) | Synchronizes time series between two PegelHub Core instances |
| [`connectors/iec-connector/`](connectors/iec-connector/) | Exchanges values over IEC 60870-5-104 |
| [`connectors/ma-connector/`](connectors/ma-connector/) | Reads milliampere inputs from a Revolution Pi |
| [`connectors/tstp-connector/`](connectors/tstp-connector/) | Exchanges measurements with a TSTP endpoint |
| [`deploy/staging/`](deploy/staging/) | Staging Compose topology and deployment, rollback, smoke, and Keycloak bootstrap scripts |
| [`deploy/ansible/`](deploy/ansible/) | One-time staging-host bootstrap |
| [`docs/architecture/pegelhub-domain-model.md`](docs/architecture/pegelhub-domain-model.md) | Domain model and current HTTP surface |
| [`docs/adr/`](docs/adr/) | Architecture decisions |

## Prerequisites

- Java 21
- Maven 3.8 or newer
- Docker Engine with Docker Compose v2
- `curl` for the local health checks

Docker is also required by the Core integration tests.

## Start locally

From the repository root:

```bash
test -f core/.env || cp core/.env.example core/.env
scripts/local-stack.sh compose-up
scripts/local-stack.sh health
```

The copied file contains disposable local-development values. Do not reuse
them outside the local stack or commit real credentials to `.env` files.
This starts Core and its databases/identity dependencies; protocol connectors
are configured and run separately from their guides in the repository map.

The stack exposes:

- Core API: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- management health: `http://localhost:8081/actuator/health`
- Keycloak: `http://pegelhub-keycloak.test:8082`
- PostgreSQL: `localhost:5444`
- InfluxDB: `http://localhost:8111`

Add `127.0.0.1 pegelhub-keycloak.test` to the local hosts file when the host
machine or browser must resolve the Keycloak issuer. See the
[`core/` guide](core/) for individual services, configuration, database
behavior, IDE runs, and troubleshooting.

Stop the containers without deleting their named volumes:

```bash
scripts/local-stack.sh compose-down
```

## Build and validate

Run these commands from the repository root.

Run the same Maven reactor verification used by pull-request CI:

```bash
mvn -B -ntp -Pintegration verify
```

For a faster module-level loop:

```bash
mvn -B -ntp -f core/pom.xml test
mvn -B -ntp -f connectors/pom.xml test
scripts/local-stack.sh compose-config
```

The integration profile starts Testcontainers and therefore needs a running
Docker daemon. CI also validates the local and staging Compose models and the
staging deployment policy tests; see [`.github/workflows/ci.yml`](.github/workflows/ci.yml)
for the complete current sequence.

## API and security

Core serves the generated OpenAPI contract in English and German:

- English JSON: `http://localhost:8080/v3/api-docs?lang=en`
- German JSON: `http://localhost:8080/v3/api-docs?lang=de`
- English YAML: `http://localhost:8080/v3/api-docs.yaml?lang=en`
- German YAML: `http://localhost:8080/v3/api-docs.yaml?lang=de`

The staging Caddy configuration proxies the complete dedicated API hostname to
Core, so the same documentation is available through these public routes when
the staging stack and its DNS records are active:

- Swagger UI: `https://${PEGELHUB_API_HOSTNAME}/swagger-ui.html`
- English JSON: `https://${PEGELHUB_API_HOSTNAME}/v3/api-docs?lang=en`
- German JSON: `https://${PEGELHUB_API_HOSTNAME}/v3/api-docs?lang=de`
- English YAML: `https://${PEGELHUB_API_HOSTNAME}/v3/api-docs.yaml?lang=en`
- German YAML: `https://${PEGELHUB_API_HOSTNAME}/v3/api-docs.yaml?lang=de`

The concrete staging hostname is intentionally held in the ignored server
`deploy/staging/.env`; the repository contains only the hostname contract and
placeholder values. See the [staging topology](deploy/staging/#topology).

Swagger UI and the system-time endpoint are public; protected API operations
accept Keycloak bearer tokens. Core reads client roles for the
`pegelhub-core-api` audience. Runtime role values are lowercase and
colon-separated: `metadata:read`, `metadata:write`, `measurement:read`,
`measurement:write`, `telemetry:read`, `telemetry:write`, and `system:admin`.

Use the [Bruno collection](core/docs/api/bruno/) for executable API examples.
The [Core guide](core/#security-model) explains the authorization model without
embedding credentials or environment-specific enrollment details here.

The metadata hierarchy is Station Owner -> Station -> Measuring Point -> Time
Series. Measurements are observed values for a time series; telemetry is
technical connector/runtime data. The
[domain model](docs/architecture/pegelhub-domain-model.md) defines the entities,
relationships, and current API routes.

## Further documentation

- [Core development and operations](core/)
- [Connector library](connectors/library/)
- [Local Keycloak development](core/docs/keycloak-local-dev.md)
- [Flyway rollout](core/docs/flyway.md)
- [InfluxDB buckets and retention](core/docs/influxdb.md)
- [Staging deployment](deploy/staging/)
- [Staging host bootstrap](deploy/ansible/)
