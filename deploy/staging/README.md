# Staging deployment

This directory defines PegelHub's currently supported remote environment: a
single-host Docker Compose staging stack. The backend repository publishes Core
and connector images; the frontend repository publishes its own image. Both
release paths invoke versioned scripts from this directory on the staging host.

This is staging documentation. The repository does not define a production
environment or a production deployment workflow.

Unless noted otherwise, commands run from the repository root. Host preparation,
deployment, rollback, Keycloak bootstrap, and smoke commands run on the staging
host. Compose syntax and repository policy tests can run on a development or CI
machine.

## Prerequisites

- a Debian or Ubuntu host prepared through [Ansible](../ansible/) or an
  equivalent Docker Engine and Compose v2 installation
- public DNS control for the frontend, API, and Keycloak hostnames
- inbound ports `80` and `443` available for Caddy and certificate issuance
- a repository checkout owned by the deploy user
- GHCR read access on the host when packages are private
- `curl` and `openssl` on the host

## Topology

The base [`compose.yaml`](compose.yaml) contains:

- Caddy on host ports `80` and `443`
- Core and its PostgreSQL metadata database
- InfluxDB and the one-shot bucket policy reconciler
- Keycloak and its PostgreSQL database
- the FTP connector

[`frontend.compose.yaml`](frontend.compose.yaml) adds the independently released
frontend to the same Compose project and network. Only Caddy publishes host
ports; application, management, database, Keycloak management, and connector
traffic remains on the internal network.

Three public DNS names route to the internal services:

```text
PEGELHUB_FRONTEND_HOSTNAME -> frontend:80
PEGELHUB_API_HOSTNAME      -> core-app:8080
PEGELHUB_KEYCLOAK_HOSTNAME -> keycloak:8080
```

Before the first frontend release, Caddy returns `503` for the frontend host
instead of exposing Core. All runtime services use Docker's `json-file` logging
driver with `10m` files and five rotations. The offline Keycloak importer is a
separate one-shot operation. Named volumes hold Caddy, PostgreSQL, InfluxDB,
and Keycloak state.

## Prepare a host

Use the [Ansible bootstrap](../ansible/) for a Debian or Ubuntu host. It installs
Docker, creates the deploy user, checks out the repository, creates the ignored
runtime directories, and initializes missing server-local secrets.

For a manual checkout, create and initialize the ignored environment file:

```bash
deploy/staging/scripts/sync-env-template.sh
deploy/staging/scripts/init-env-secrets.sh
```

These scripts preserve existing values. The sync script appends new keys from
`.env.example`; the initialization script replaces empty or staging-placeholder
secret values without printing generated secrets.

In `deploy/staging/.env`:

1. replace all three example hostnames and create matching DNS records;
2. set an immutable backend tag such as `sha-<short-sha>` or `v0.1.0`;
3. keep `PEGELHUB_ENVIRONMENT=staging` and
   `PEGELHUB_DEPLOY_MARKER=pegelhub-staging`;
4. keep `FLYWAY_BASELINE_ON_MIGRATE=false` except during the documented
   one-time adoption of a verified legacy schema;
5. review InfluxDB retention before the first data-bearing deploy.

Log in to GHCR on the host if the packages are private. Runtime secrets remain
in the host's ignored `.env` and `ftp-config/`; they are not copied into GitHub,
the repository, or rendered Compose artifacts.

## FTP connector configuration

Create a private configuration directory on the host:

```bash
mkdir -p deploy/staging/ftp-config/mappings
chmod 700 deploy/staging/ftp-config
```

It needs `connector.yaml` and exactly one mapping file. The in-stack addresses
are Core at `http://core-app:8080/` and the public Keycloak token endpoint at
`https://${PEGELHUB_KEYCLOAK_HOSTNAME}/realms/pegelhub/protocol/openid-connect/token`.
Use the [FTP connector guide](../../connectors/ftp-connector/) for the complete
schema.

The staging realm seed deliberately contains no FTP client or client secret.
While the connector is stopped, create a confidential service-account client
with browser and direct-access flows disabled and full scope disabled. Remove
inherited scopes; assign exactly `pegelhub-core-roles`,
`pegelhub-core-audience`, and `pegelhub-client-actor` as default scopes, with no
optional scopes. Assign only the Core roles `measurement:write` and
`telemetry:write` to both the service account and its scope role mappings.
Verify that its token:

- has the configured issuer;
- has only `pegelhub-core-api` as its audience;
- identifies the expected client in `azp`;
- contains only the lowercase Core roles above;
- has `pegelhub_actor_type` set to `CLIENT`.

Register the same client ID through Core's admin connector endpoint before
starting ingestion. The target time series must name that Connector as its
source and have a `WRITE` access grant for it. Write the client secret directly
into the ignored `connector.yaml`, set that file to mode `0600`, and never place
the secret in shell history, logs, tickets, or Git. The
[Bruno write workflow](../../core/docs/api/bruno/#write-workflow) demonstrates
the required metadata sequence.

## Database and retention changes

Flyway owns the metadata schema. Fresh databases use
`FLYWAY_BASELINE_ON_MIGRATE=false`. Before the first Flyway deployment against
an existing Hibernate-created schema, follow the backup, schema comparison,
orphan check, one-start baseline, and verification steps in the
[Flyway rollout guide](../../core/docs/flyway.md). A rollback does not undo
Flyway migrations.

Measurement and telemetry retention are independent:

```env
INFLUX_DATA_RETENTION=60d
INFLUX_TELEMETRY_RETENTION=60d
```

Accepted values are `0s` for infinite retention or positive whole hours, days,
or weeks such as `24h`, `60d`, or `8w`. Reducing retention can permanently
expire older data; an image rollback cannot restore it. See the
[InfluxDB guide](../../core/docs/influxdb.md).

## Validate before deployment

Compose syntax can be checked with the placeholder environment without
printing resolved values:

```bash
docker compose --env-file deploy/staging/.env.example \
  -f deploy/staging/compose.yaml config --quiet

PEGELHUB_FRONTEND_IMAGE=ghcr.io/viadonau/pegelhub-frontend@sha256:0000000000000000000000000000000000000000000000000000000000000000 \
docker compose --env-file deploy/staging/.env.example \
  -f deploy/staging/compose.yaml \
  -f deploy/staging/frontend.compose.yaml config --quiet

docker compose --env-file deploy/staging/.env.example \
  -f deploy/staging/compose.yaml \
  -f deploy/staging/keycloak-bootstrap.compose.yaml \
  --profile keycloak-bootstrap config --quiet
```

On the host, validate real configuration and an existing image tag without
changing services:

```bash
deploy/staging/scripts/deploy.sh --check sha-<short-sha>
```

The deploy validation rejects staging markers or hostnames that are missing or
placeholders, missing or known mutable/placeholder image tags, invalid
retention values, missing FTP configuration, unexpected public ports, and
`build:` sections. It validates Compose quietly and does not persist a rendered
configuration.

Repository policy tests are:

```bash
deploy/staging/tests/keycloak-static-test.sh
deploy/staging/tests/frontend-deployment-test.sh
deploy/staging/tests/keycloak-bootstrap-integration.sh
```

The Keycloak integration test creates and removes a unique disposable Compose
project and its volumes.

## Deploy and roll back backend images

Deploy an immutable image tag from the host:

```bash
deploy/staging/scripts/deploy.sh sha-<short-sha>
```

The script validates configuration, pulls the base stack images, updates the
services, records current and previous backend tags under the ignored `state/`
directory, and runs staging smoke checks. It preserves the separately managed
frontend service.

Force-recreate Keycloak only when a theme or container configuration change
needs reloading:

```bash
deploy/staging/scripts/deploy.sh --refresh-keycloak sha-<short-sha>
```

This option does not import, migrate, or reset a realm.

Roll back to the recorded previous backend tag, or deploy a known tag:

```bash
deploy/staging/scripts/deploy.sh --rollback
deploy/staging/scripts/deploy.sh sha-<previous-short-sha>
```

Rollback changes images and services only. It does not remove volumes, reverse
database migrations, or restore data expired by retention.

## Deploy and roll back the frontend

The frontend activation script accepts only a digest-pinned image from the
expected GHCR repository:

```bash
deploy/staging/scripts/deploy-frontend.sh \
  ghcr.io/viadonau/pegelhub-frontend@sha256:<64-lowercase-hex>
```

It pulls and recreates only `frontend`, waits for health, then checks the public
frontend and its API proxy. A failed activation restores the previous image; a
failed first release removes the container so Caddy returns the undeployed
`503`. Successful image references are stored in ignored `state/` data.

Roll back only the frontend:

```bash
deploy/staging/scripts/deploy-frontend.sh --rollback
```

## Keycloak bootstrap

Normal deployment never imports a realm. For a new or deliberately emptied
Keycloak database, stop Keycloak and run the explicit offline bootstrap:

```bash
deploy/staging/scripts/bootstrap-keycloak.sh
```

The script refuses to run while Keycloak is online. It starts the Keycloak
database, imports [`keycloak/pegelhub-realm.json`](keycloak/pegelhub-realm.json)
with overwrite disabled, and starts Keycloak without startup import. The seed
contains the API roles/scopes, Core resource client, public frontend client,
theme, and German locale; it contains no users, service-client secrets, local
clients, or localhost origins.

Bootstrap, backend deployment, and frontend deployment share a host lock.
Re-running the importer skips an existing realm rather than overwriting
identity state. Realm reset or database-volume deletion is a separate,
deliberate data-loss operation and is never part of routine deployment.

## Smoke checks

Run the staging checks independently after a deploy:

```bash
deploy/staging/scripts/smoke.sh
```

They verify the public Core system-time route, public Keycloak discovery,
internal Core and Keycloak health, and a running FTP connector container. The
frontend deployment adds public frontend and frontend API-proxy checks.

## GitHub workflows

The backend [`Images`](../../.github/workflows/images.yml) workflow publishes
Core and all five connector images. Pushes to backend `main`, `v*` tags, and
eligible manual runs deploy the matching tag to the GitHub `staging`
Environment. The separate
[`Deploy Frontend`](../../.github/workflows/deploy-frontend.yml) workflow accepts
the frontend repository's digest-pinned release request.

Both paths use the same staging SSH configuration, GitHub Environment, workflow
concurrency group, server checkout, and host lock. Required GitHub values are
listed in the [Ansible guide](../ansible/#github-staging-environment). Use
GitHub Environment required reviewers when a manual approval gate is desired.

## Operational cautions

- Never run `docker compose down -v` as part of routine operation.
- Do not use `--remove-orphans`; the frontend is a separately managed service
  in the same Compose project.
- Keep known-good backend and frontend images available on the host.
- Treat Flyway adoption and retention reductions as data changes, not ordinary
  image configuration.
- Normal Keycloak startup and deployment must not import or reset the realm.
- The FTP connector may write real measurements whenever its mounted
  configuration is active; staging smoke checks are not a data-isolation
  boundary.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Caddy cannot obtain a certificate | All three DNS records, public reachability of ports `80`/`443`, and Caddy logs |
| Image pull is denied | Host GHCR login and package read permission |
| A service is unhealthy | `docker compose --env-file deploy/staging/.env -f deploy/staging/compose.yaml ps` and that service's logs |
| Script reports another staging operation | Confirm no deploy or bootstrap is active before treating the directory under `deploy/staging/state/` as a stale lock |
| Smoke fails on Core or Keycloak | Public DNS/TLS first, then internal actuator or Keycloak management health and container logs |
