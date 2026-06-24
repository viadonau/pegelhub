# PegelHub Staging Deployment

This directory is the first CD slice for PegelHub: a staging/rehearsal stack that
pulls already-published GHCR images and starts them with Docker Compose. GitHub
builds and publishes images, then the `Images` workflow can SSH into staging and
run the host-side deploy script.

## Services

- Caddy reverse proxy on ports `80` and `443`
- Optional frontend from `PEGELHUB_FRONTEND_IMAGE`
- PegelHub Core from `ghcr.io/viadonau/pegelhub-core:${PEGELHUB_IMAGE_TAG}`
- PostgreSQL metadata database
- InfluxDB
- Keycloak plus Keycloak PostgreSQL database
- FTP connector from `ghcr.io/viadonau/pegelhub-ftp-connector:${PEGELHUB_IMAGE_TAG}`

Only Caddy publishes host ports. The frontend, Core, actuator, databases,
InfluxDB, Keycloak management, and the FTP connector stay on the internal
Compose network.

Public traffic uses separate hostnames:

```text
PEGELHUB_FRONTEND_HOSTNAME -> frontend:80
PEGELHUB_API_HOSTNAME      -> core-app:8080
PEGELHUB_KEYCLOAK_HOSTNAME -> keycloak:8080
```

The frontend container is optional until a real frontend image exists. Caddy
already owns the frontend hostname; without the `frontend` Compose profile, that
hostname returns a temporary `503` response instead of exposing Core directly.

## Host Bootstrap

The preferred bootstrap path is Ansible:

```sh
ansible-playbook -i deploy/ansible/inventory/staging.ini deploy/ansible/staging.yml
```

See `deploy/ansible/README.md` for inventory and variable setup. The Ansible
playbook installs Docker, creates the deploy user, clones this repository,
creates the ignored staging config directories, and initializes missing
server-local secrets in `deploy/staging/.env`.

Create DNS records for the frontend, API, and Keycloak hostnames from `.env` so
all three names point to the staging host. Caddy uses those hostnames for
routing and certificate issuance.

If GHCR packages are private, log in once on the host with a token that can read
packages:

```sh
echo "<github-token>" | docker login ghcr.io -u "<github-user>" --password-stdin
```

Clone this repository onto the staging host. The GitHub workflow expects
`STAGING_REPO_DIR` to point to this checkout:

```sh
git clone https://github.com/viadonau/pegelhub.git /opt/pegelhub
```

If you bootstrap without Ansible, create or sync the staging env file and then
initialize missing server-local secrets:

```sh
deploy/staging/scripts/sync-env-template.sh
deploy/staging/scripts/init-env-secrets.sh
```

Replace the remaining hostnames and image placeholders in `.env`. Keep
`PEGELHUB_ENVIRONMENT=staging` and
`PEGELHUB_DEPLOY_MARKER=pegelhub-staging`; the deploy script checks those before
it changes services. The init script does not overwrite existing real secret
values and does not print generated secrets. The sync script appends missing
keys from `.env.example` but preserves the values already present in `.env`.

## GitHub Staging Deploy Setup

The `Images` workflow has a `Deploy Staging` job. It runs only after all images
are published successfully.

Configure a GitHub Environment named `staging`, then add these environment
variables:

- `STAGING_REPO_DIR`: repository checkout on the staging host, for example `/opt/pegelhub`
- `STAGING_SSH_HOST`: staging host DNS name or IP
- `STAGING_SSH_PORT`: SSH port, usually `22`
- `STAGING_SSH_USER`: SSH user used for deployment

Add these environment secrets:

- `STAGING_SSH_PRIVATE_KEY`: private key for the staging deploy user
- `STAGING_SSH_FINGERPRINT`: one SHA256 fingerprint of the staging host key

Create a dedicated staging deploy key, then install the public key for the
deploy user on the staging host:

```sh
ssh-keygen -t ed25519 -C "github-actions-pegelhub-staging" -f pegelhub-staging-deploy
```

Store the private key contents in `STAGING_SSH_PRIVATE_KEY`. Add the public key
to the deploy user's `~/.ssh/authorized_keys` on the staging host.

Generate the host fingerprints from a trusted machine:

```sh
ssh-keyscan -p <port> <host> 2>/dev/null | ssh-keygen -lf -
```

The output usually contains multiple host key types, for example RSA, ECDSA,
and ED25519. Store exactly one fingerprint in `STAGING_SSH_FINGERPRINT`, with no
quotes and no extra lines. Use the ECDSA `SHA256:...` fingerprint for the
default staging setup.

The staging deploy user needs access to Docker, the repository checkout, and the
ignored staging files under `deploy/staging/`. If Docker access is granted
through group membership, log out and back in on the host before testing the
deploy user. Runtime secrets remain on the staging host in `.env` and
`ftp-config/`; they are not copied into GitHub.

Use GitHub Environment required reviewers if you want staging deployment to wait
for manual approval after the image build succeeds.

When the workflow runs from `main`, staging deploys the published
`sha-<short-sha>` image tag for that commit. When the workflow runs from a
release tag such as `v0.1.0`, staging deploys that tag. Manual workflow runs
deploy to staging by default, but the `deploy_staging` input can be disabled for
image-only tests.

## FTP Connector Config

Create the FTP config directory on the staging host:

```sh
mkdir -p deploy/staging/ftp-config
chmod 700 deploy/staging/ftp-config
```

The directory must contain:

- `connector.properties`
- `pegelhub.yaml`

Use staging secrets in those files. Do not commit them. A typical in-stack
configuration uses:

```properties
core.address=core-app
core.port=8080
```

For Keycloak, use the public issuer hostname from `.env` so the token issuer
matches what Core validates:

```yaml
keycloak:
  tokenUrl: "https://auth-pegelhub-staging.example.com/realms/pegelhub/protocol/openid-connect/token"
```

The FTP credentials also belong in this mounted config, not in Git and not in
the Docker image.

## Frontend

The Compose stack includes an optional `frontend` profile:

```env
COMPOSE_PROFILES=frontend
PEGELHUB_FRONTEND_IMAGE=ghcr.io/viadonau/pegelhub-frontend:sha-<short-sha>
```

Leave `COMPOSE_PROFILES` unset until a frontend image exists. Once enabled,
Caddy routes `PEGELHUB_FRONTEND_HOSTNAME` to `frontend:80`, while API calls
should target `https://${PEGELHUB_API_HOSTNAME}` and Keycloak should use
`https://${PEGELHUB_KEYCLOAK_HOSTNAME}/realms/pegelhub`. The current image
workflow does not build a frontend image yet, so
`PEGELHUB_FRONTEND_IMAGE` must point to a separately published image until a
frontend module is added to the repository.

## Keycloak Theme And Realm

Staging bind-mounts the login theme from
`core/docker/keycloak/themes` into the Keycloak container. Normal deploys keep
the existing Keycloak container running unless Docker Compose needs to change
it. When deploying Keycloak theme or container config changes, refresh Keycloak
explicitly so production theme caches and mounts are reloaded:

```sh
deploy/staging/scripts/deploy.sh --refresh-keycloak sha-<short-sha>
```

Compose passes `PEGELHUB_FRONTEND_URL=https://${PEGELHUB_FRONTEND_HOSTNAME}` to
Keycloak. Fresh realm imports use that value for the `pegelhub-frontend`
client's root URL, redirect URIs, and web origins.

Realm import still only creates a missing realm. It does not update existing
staging realm state. For an existing staging Keycloak database, apply realm,
client, client-scope, and theme-setting changes deliberately through the
Keycloak admin UI or `kcadm` after taking the normal staging backup.

## Validate

Render Compose without changing services:

```sh
docker compose --env-file deploy/staging/.env.example -f deploy/staging/compose.yaml config
```

Run the checked validation path with the real host config:

```sh
deploy/staging/scripts/deploy.sh --check sha-<short-sha>
```

The validation rejects missing or placeholder image tags, missing FTP config,
production-unsafe public ports, and any rendered `build:` section.

## Deploy From GitHub

Run the `Images` workflow manually or push a `v*` tag. After Core and connector
images are published, GitHub SSHs into the staging host and runs:

```sh
deploy/staging/scripts/deploy.sh <published-image-tag>
```

The workflow fails if the remote deploy or smoke checks fail.

## Deploy From The Host

Deploy an image tag that already exists in GHCR:

```sh
deploy/staging/scripts/deploy.sh sha-<short-sha>
```

For a release tag:

```sh
deploy/staging/scripts/deploy.sh v0.1.0
```

The script renders Compose, validates it, pulls images, starts the stack,
records the current and previous image tags under `deploy/staging/state/`, and
runs the smoke script. Pass `--refresh-keycloak` when the deployment needs to
force-recreate the Keycloak container.

## Smoke Checks

Run smoke checks again after a deploy:

```sh
deploy/staging/scripts/smoke.sh
```

The smoke script checks:

- Public API route through Caddy
- Public frontend route through Caddy when the `frontend` profile is enabled
- Keycloak issuer discovery through Caddy
- Core actuator health over the internal network
- Keycloak management health over the internal network
- FTP connector container is running

The FTP connector can write measurements during normal operation. Treat this
staging stack as a real ingestion environment once real FTP config is mounted.

## Rollback

Rollback to the previously recorded image tag:

```sh
deploy/staging/scripts/deploy.sh --rollback
```

Or deploy a specific known-good tag:

```sh
deploy/staging/scripts/deploy.sh sha-<previous-short-sha>
```

Rollback changes image tags and restarts services. It does not delete volumes,
prune images, or attempt database rollback. Core currently relies on Hibernate
`ddl-auto=update`, so destructive schema rollback must remain a manual
backup/restore decision.

## Operational Notes

- Do not run `docker compose down -v` unless you explicitly want to delete
  staging data.
- Keep the previous known-good image pulled on the host.
- Keycloak realm import runs on first start for a fresh Keycloak DB. Existing
  Keycloak state should be changed deliberately and backed up before risky
  auth changes.
- `--refresh-keycloak` recreates the Keycloak container, but does not recreate
  or wipe the Keycloak database.
- Rotate any real FTP password that was ever committed or shared in examples.
