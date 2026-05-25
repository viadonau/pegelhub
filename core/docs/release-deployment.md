# Release Deployment

This is the practical release path for deploying published PegelHub images into a
self-hosted Docker Compose runtime.

The release compose file uses the already-built Core image from GHCR. It does not
build Java artifacts locally. Connector images are deployed separately on the
machine that owns the hardware or protocol endpoint, for example the RevPi.

## Image Lifecycle

Pull requests run Maven checks through the CI workflow.

The Images workflow has two release-oriented modes:

- Manual run without `push`: builds images inside GitHub Actions without publishing them.
- Manual run with `push`: publishes `ci-<run_number>` and `sha-<short_sha>` tags to GHCR.
- Git tag `v*`: publishes release images to GHCR and writes the pushed digest to the
  workflow summary.

Prefer deploying an immutable digest from the workflow summary once a release image
has been validated:

```text
PEGELHUB_CORE_IMAGE=ghcr.io/<owner>/pegelhub-core@sha256:<digest>
```

A version tag is convenient for humans, but the digest is the exact artifact.

## Core Runtime

From `core/`:

```bash
cp .env.release.example .env.release
```

Edit `.env.release` and set at least:

- `PEGELHUB_CORE_IMAGE`
- database passwords
- InfluxDB admin password and token
- Keycloak admin and database passwords
- `KEYCLOAK_HOSTNAME_URL`
- `KEYCLOAK_ISSUER_URI`

Then render and start the release stack:

```bash
docker compose --env-file .env.release -f docker-compose.release.yaml config --quiet
docker compose --env-file .env.release -f docker-compose.release.yaml pull
docker compose --env-file .env.release -f docker-compose.release.yaml up -d
```

Check container state and Core health:

```bash
docker compose --env-file .env.release -f docker-compose.release.yaml ps
curl -fsS http://localhost:8081/actuator/health
```

`docker-compose.release.yaml` includes Keycloak `start-dev` and the checked-in realm
import so small self-hosted and test deployments can boot with the same local identity
shape. Before a real production deployment, replace the local bootstrap secrets and
review whether Keycloak should be managed outside this compose stack.

## RevPi mA Connector Runtime

The mA connector runs on the RevPi because it needs `/dev/piControl0`.

Use the compose example in `connectors/ma-connector/examples/docker/` and set:

```bash
export MA_CONNECTOR_IMAGE=ghcr.io/<owner>/pegelhub-ma-connector:v0.1.0
export PEGELHUB_HOST_IP=<Core and Keycloak host LAN IP>
docker compose up -d
```

For your current LAN shape, `PEGELHUB_HOST_IP` is the Mac LAN IP when Core and
Keycloak run on the Mac. It is not the RevPi IP. The connector still uses
`pegelhub-keycloak.test` in its token URL, and `extra_hosts` maps that hostname to
the host running Keycloak.

## Backups Before Deploy

Before updating a running environment, capture database backups. From `core/`:

```bash
set -a
. ./.env.release
set +a

mkdir -p backups
BACKUP_TS=$(date +%Y%m%d-%H%M%S)

docker compose --env-file .env.release -f docker-compose.release.yaml exec -T meta-db \
  pg_dump -U postgres "$META_DB" > "backups/meta-db-$BACKUP_TS.sql"

docker compose --env-file .env.release -f docker-compose.release.yaml exec -T keycloak-db \
  pg_dump -U keycloak "$KEYCLOAK_DB" > "backups/keycloak-db-$BACKUP_TS.sql"

docker compose --env-file .env.release -f docker-compose.release.yaml exec -T data-db \
  influx backup "/tmp/influx-backup-$BACKUP_TS"

docker compose --env-file .env.release -f docker-compose.release.yaml cp \
  "data-db:/tmp/influx-backup-$BACKUP_TS" "./backups/influx-backup-$BACKUP_TS"
```

## Rollback

Rollback is image selection plus restart:

1. Set `PEGELHUB_CORE_IMAGE` in `.env.release` to the previous tag or digest.
2. Run `docker compose --env-file .env.release -f docker-compose.release.yaml pull core-app`.
3. Run `docker compose --env-file .env.release -f docker-compose.release.yaml up -d core-app`.
4. Re-run the actuator health check.

For connector rollback, set `MA_CONNECTOR_IMAGE` to the previous connector image and
restart the connector compose stack on the RevPi.

## Smoke Checks

After Core starts:

```bash
curl -fsS http://localhost:8081/actuator/health
```

Then request a connector token with the deployed connector client and send one
controlled measurement from a test connector or the RevPi connector. Connector smoke
checks may create measurements, so use known test input metadata when validating a
non-production environment.
