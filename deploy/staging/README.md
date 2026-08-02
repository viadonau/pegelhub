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
Neither host bootstrap nor a normal image deploy imports a Keycloak realm or
provisions clients.

## Metadata Schema Rollout

Core owns the metadata schema through Flyway. The staging env template keeps
`FLYWAY_BASELINE_ON_MIGRATE=false`, and the sync script appends that setting to
older host `.env` files without changing existing values.

Before the first deployment against an existing Hibernate-created database,
follow the complete [Flyway rollout procedure](../../core/docs/flyway.md). It
requires a backup and schema/orphan preflight, enables the baseline setting for
the first Flyway startup only, verifies the version 1 baseline and version 2
migration history, and then disables the setting again.

## InfluxDB Retention

Staging configures measurement and telemetry retention independently:

```env
INFLUX_DATA_RETENTION=60d
INFLUX_TELEMETRY_RETENTION=60d
```

Use `0s` for infinite retention. Other accepted values are positive whole
hours, days, or weeks, such as `24h`, `60d`, or `8w`.

The `influx-bucket-setup` service reconciles both policies on fresh and existing
InfluxDB volumes before Core starts. Reducing retention, including changing
`0s` to a finite value, can permanently remove older points. Back up data that
must be preserved before deploying such a change. An application rollback does
not restore expired InfluxDB data.

## Container Console Logs

Every staging service, including the optional frontend and the one-shot
`influx-bucket-setup` service, uses Docker's `json-file` logging driver with a
maximum file size of `10m` and five files per container. This bounds
Docker-managed stdout and stderr logs to approximately 50 MB per container. It
does not change application log levels or retention inside databases and named
volumes.

Docker applies logging options when it creates a container. On the first deploy
of this policy, `docker compose up -d` should detect the changed service
configuration and recreate every active staging container, causing a brief
service interruption while preserving named volumes. The idempotent
`influx-bucket-setup` service runs again before Core starts. If deployment output
shows that an existing container was not recreated, rerun the stack explicitly
with the same immutable image tag:

```sh
PEGELHUB_IMAGE_TAG=sha-<short-sha> docker compose \
  --env-file deploy/staging/.env \
  -f deploy/staging/compose.yaml \
  up -d --force-recreate
deploy/staging/scripts/smoke.sh
```

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

- `connector.yaml`
- `mappings/` with exactly one FTP mapping YAML file

Use staging secrets in those files and do not commit them. After manually
enrolling the FTP connector in Keycloak, a typical in-stack configuration is:

```yaml
core:
  baseUrl: "http://core-app:8080/"
  authentication:
    tokenUrl: "https://auth-pegelhub-staging.example.com/realms/pegelhub/protocol/openid-connect/token"
    clientId: "pegelhub-staging-ftp-connector"
    clientSecret: "<generated-client-secret>"
polling:
  interval: "15m"
mappings:
  directory: "mappings"
ftp:
  server:
    host: "ftp.example.com"
    port: 21
    authentication:
      username: "pegelReader"
      password: "replace-with-staging-secret"
  source:
    directory: "/"
    parserType: "zrxp"
```

Keep the config directory at mode `0700` and `connector.yaml` at mode `0600`.
Replace the example token hostname with `PEGELHUB_KEYCLOAK_HOSTNAME`. Enter the
generated client secret directly with an editor on the staging host rather than
placing it in shell history or command output. FTP and Keycloak credentials
belong only in this ignored mounted config, not in `.env`, Git, or the Docker
image. See the FTP connector README for the mapping file shape.

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

## Keycloak Bootstrap

Staging bind-mounts the login theme from
`core/docker/keycloak/themes` into the Keycloak container, but it does not mount
the local-development realm import. The dedicated staging seed is
`deploy/staging/keycloak/pegelhub-realm.json`. It contains the API roles and
token scopes, Core resource client, public frontend client, PegelHub login
theme, and German locale. It contains no users, client secrets, `local-*`
clients, or localhost origins.

The offline importer lives only in `keycloak-bootstrap.compose.yaml`. Routine
Compose and `deploy.sh` never load that file, so an inherited Compose profile
cannot activate a realm import.

The normal Keycloak command is only `start`. Image deploys and container
recreation never import, overwrite, or migrate a realm. When deploying theme or
container config changes, `--refresh-keycloak` only force-recreates Keycloak so
the theme mount and production caches reload:

```sh
deploy/staging/scripts/deploy.sh --refresh-keycloak sha-<short-sha>
```

That option is not a realm migration and does not apply seed changes to an
existing realm.

For a new or deliberately emptied Keycloak database, stop Keycloak and run:

```sh
deploy/staging/scripts/bootstrap-keycloak.sh
```

The script refuses to run while Keycloak is online. It starts only the Keycloak
database, performs an offline seed import with `--override=false`, starts
Keycloak without startup import, and stops there. The frontend origin is derived
from `https://${PEGELHUB_FRONTEND_HOSTNAME}` during that import. Re-running the
offline import against an existing realm skips it rather than overwriting
identity state. This follows the
[Keycloak import/export lifecycle guidance](https://www.keycloak.org/server/importExport).
The explicit scripts reject placeholder, loopback, or malformed public
hostnames. Bootstrap and routine deploy serialize through
`state/keycloak-bootstrap.lock`, so Compose cannot start Keycloak while an offline
import is using its database. If a process is killed without running its cleanup
trap, verify that neither operation is active before removing that stale lock
directory.

### Manual FTP Connector Enrollment

The staging seed deliberately contains no FTP client or secret. After a fresh
realm bootstrap, keep the FTP connector stopped and enroll it manually:

1. In the Keycloak admin console, create the confidential client
   `pegelhub-staging-ftp-connector`. Enable client authentication and service
   accounts; disable standard, implicit, and direct-access grants.
2. Disable full scope. Remove automatically inherited default and optional
   scopes, then assign exactly `pegelhub-core-roles`,
   `pegelhub-core-audience`, and `pegelhub-client-actor` as default scopes. Keep
   optional scopes and client-specific protocol mappers empty.
3. Assign only the Core client roles `measurement:write` and `telemetry:write`
   under Service account roles. With full scope disabled, allow those same two
   Core roles in the client's scope role mappings and no others. Do not assign
   realm roles, groups, or roles from another client.
4. Put the generated secret directly into the ignored
   `ftp-config/connector.yaml` structure shown above. Keep the directory at mode
   `0700` and the file at mode `0600`; do not paste the secret into commands,
   logs, tickets, or Git.
5. Before starting FTP, perform a client-credentials grant from the staging host
   without shell tracing or credential output. Decode the token locally without
   sending it to an external service. Verify `azp` is the FTP client, `aud`
   contains only `pegelhub-core-api`, `pegelhub_actor_type` is `CLIENT`, Core
   roles are exactly `measurement:write` and `telemetry:write`, and the token has
   no realm or other-resource roles.
6. Register the same client ID in Core, start the FTP connector, and verify one
   authenticated write.

Secret rotation follows the same manual Keycloak and server-local configuration
procedure. Automated connector provisioning remains intentionally out of scope.

### Deliberate Fresh Staging Reset

This repository change does not reset live staging. After merge, schedule a
maintenance window and perform the reset separately:

1. Delete any legacy `deploy/staging/state/compose.rendered.yaml`; older deploy
   scripts stored protected environment values there. Rotate those staging
   values first if that file was readable beyond the intended deploy account.
2. Back up the Keycloak PostgreSQL database and the ignored
   `deploy/staging/ftp-config/connector.yaml`.
3. Confirm the Compose project is the staging project and stop
   `ftp-connector`, `core-app`, `keycloak`, and `keycloak-db`.
4. Remove only the stopped Keycloak containers. Locate the volume carrying both
   Compose labels `com.docker.compose.project=pegelhub-staging` and
   `com.docker.compose.volume=keycloak-data`, verify it manually, and remove only
   that volume. Do not use `docker compose down -v`.
5. Run `deploy/staging/scripts/bootstrap-keycloak.sh`. It creates the fresh
   database volume, imports the seed, and starts Keycloak without provisioning
   any service clients.
6. Verify the realm, complete the manual FTP connector enrollment above, then
   deploy or start the remaining stack and run the staging smoke checks.

The old staging realm is not migrated by this path. Its deletion is the
separate, deliberate data-loss operation in step 4.

## Validate

Validate Compose without rendering environment values:

```sh
docker compose --env-file deploy/staging/.env.example \
  -f deploy/staging/compose.yaml config --quiet
docker compose --env-file deploy/staging/.env.example \
  -f deploy/staging/compose.yaml \
  -f deploy/staging/keycloak-bootstrap.compose.yaml \
  --profile keycloak-bootstrap config --quiet
```

Run the checked validation path with the real host config:

```sh
deploy/staging/scripts/deploy.sh --check sha-<short-sha>
```

The validation rejects missing or placeholder image tags, missing FTP config,
invalid InfluxDB retention values, production-unsafe public ports, and any
`build:` section. It validates Compose quietly and never persists a rendered
configuration containing values from the protected `.env`. The first non-check
deploy after this change also deletes the legacy rendered artifact produced by
older versions. A mutating deploy refuses to run while a Keycloak bootstrap
holds the shared lock; `--check` remains read-only and can run concurrently.

Run the dedicated policy checks:

```sh
deploy/staging/tests/keycloak-static-test.sh
deploy/staging/tests/keycloak-bootstrap-integration.sh
```

The integration check creates a unique disposable Compose project and removes
only that project's containers and volumes.

## Deploy From GitHub

Push to `main`, run the `Images` workflow manually, or push a `v*` tag. After
Core and connector images are published, GitHub SSHs into the staging host and runs:

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

The script validates Compose, pulls images, starts the stack,
records the current and previous image tags under `deploy/staging/state/`, and
runs the smoke script. It does not retain the rendered Compose model. Pass
`--refresh-keycloak` only when the deployment needs to force-recreate the
Keycloak container for theme or container-config reload.

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
prune images, or undo Flyway migrations. Database rollback remains a deliberate
backup/restore or forward-migration decision.

## Operational Notes

- Do not run `docker compose down -v` unless you explicitly want to delete
  staging data.
- Keep the previous known-good image pulled on the host.
- Normal Keycloak startup never imports a realm. Use the explicit offline
  bootstrap only with Keycloak stopped.
- `--refresh-keycloak` recreates the Keycloak container for theme/config reload;
  it is not a realm import, migration, reset, or database wipe.
- Container logging options take effect only after container recreation; verify
  that every active service is recreated on the first deployment of a logging
  policy change.
- Rotate any real FTP password that was ever committed or shared in examples.
- Treat retention reductions as data migrations; image rollback does not undo
  expired InfluxDB data.
