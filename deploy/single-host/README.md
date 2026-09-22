# Single-Host Deployment

Run the PegelHub platform on one host using published container images. The
base stack contains Core, PostgreSQL metadata storage, InfluxDB, Keycloak with
its own PostgreSQL database, and Caddy. Only Caddy publishes host ports: HTTP
`80` and HTTPS `443`. Database and management ports remain internal.

The [frontend](../../frontend/README.md) is released separately into the same
Compose project. Protocol connectors use the [connector runner](../connector/README.md)
in separate projects. For local development, use the
[Core development stack](../../core/README.md), not this deployment.

All commands below run from the repository root **on the deployment host**.
They can change running services unless explicitly marked as validation.

## Prerequisites

- Docker Engine and a current Docker Compose plugin with `up --wait` support.
- A deployment account with Docker access and ownership of its configuration
  and state directories; `curl` and `openssl` must be available.
- Three distinct DNS hostnames for the frontend, Core API, and Keycloak, all
  resolving to this host. Supply hostnames without a scheme, port, or path.
- Available ports `80` and `443`; for automatic certificates, DNS and network
  access must allow Caddy to obtain and renew certificates.
- Published Core and frontend images for the intended release, plus registry
  authentication on the host if the packages are private.

The [Ansible bootstrap](../ansible/README.md) prepares the supported staging
host layout. Other installations must prepare the directories and their
ownership themselves.

## Configuration And Storage

Host-specific configuration and release records live outside the Git checkout:

```text
/etc/pegelhub/<deployment>/
  pegelhub.env
  tls/server/
  tls/trust/

/var/lib/pegelhub/<deployment>/state/
```

Choose one deployment name and export its paths in every operational shell.
Replace `my-deployment` below; the staging workflow uses `staging`.

```sh
export PEGELHUB_CONFIG_DIR=/etc/pegelhub/my-deployment
export PEGELHUB_STATE_DIR=/var/lib/pegelhub/my-deployment/state
```

The deployment account must be able to create and write these directories.
Create the TLS directories even when using automatic certificates and system
trust. Then initialize the environment from
[`pegelhub.env.example`](pegelhub.env.example):

```sh
mkdir -p "$PEGELHUB_CONFIG_DIR/tls/server" "$PEGELHUB_CONFIG_DIR/tls/trust" \
  "$PEGELHUB_STATE_DIR"
chmod 700 "$PEGELHUB_CONFIG_DIR/tls/server"
deploy/single-host/scripts/sync-env-template.sh
deploy/single-host/scripts/init-env-secrets.sh
```

`sync-env-template.sh` creates `pegelhub.env` when missing, or appends missing
keys without replacing existing values. `init-env-secrets.sh` fills missing or
placeholder database passwords, the InfluxDB token, and the Keycloak bootstrap
admin password. It preserves initialized values and does not rotate credentials
in existing databases. The environment file is restricted to mode `0600`.

Review `pegelhub.env` before continuing:

- Set a unique, stable `COMPOSE_PROJECT_NAME`. Staging uses
  `pegelhub-staging`; changing it creates a different set of named volumes.
- Replace all three example hostnames and `PEGELHUB_IMAGE_TAG`. Use a published
  release tag such as `v0.1.0` or a commit tag such as `sha-42bd19b`, not `latest`.
- Select the TLS and trust modes below. Optional `PEGELHUB_TLS_SERVER_DIR` and
  `PEGELHUB_TRUST_DIR` values should be absolute paths.
- Review retention: measurement and telemetry buckets default to `60d`, while
  `INFLUX_LATEST_RANGE` defaults to `72h`. See the
  [InfluxDB guide](../../core/docs/influxdb.md).

Database contents and Caddy certificate state are held in Docker named volumes,
not in `PEGELHUB_STATE_DIR`. The state directory contains deployment locks and
release records, not backups. Keep configuration, private keys, credentials,
and backup material out of Git.

## TLS And Trust

| Installation | `PEGELHUB_TLS_MODE` | `PEGELHUB_TRUST_MODE` |
| --- | --- | --- |
| Public/default | `automatic` | `system` |
| Provided certificate | `provided` | `system` |
| Provided certificate and private CA | `provided` | `custom` |

`automatic` uses Caddy's built-in ACME support. `provided` loads PEM bundles
from `tls/server/current`. After setting `PEGELHUB_TLS_MODE=provided`, install
one shared SAN certificate/key pair or pass multiple named pairs in the same
command. Together, they must cover all three configured hostnames:

```sh
deploy/single-host/scripts/install-certificates.sh \
  /private/incoming/shared.fullchain.pem \
  /private/incoming/shared.privkey.pem
```

Input names must match `<name>.fullchain.pem` and `<name>.privkey.pem`. The
installer checks expiration, hostname coverage, and matching private keys,
replaces the installed set, and reloads Caddy when it is already running.
Provided certificates require operator-managed renewal; rerun the installer
with the complete replacement set when rotating them.

`custom` adds the `*.crt` files in the platform's `tls/trust` directory to the
Core Java truststore without removing the image's public roots. Each
independently deployed connector uses its own `trust/` directory. Managed
browsers must trust the company CA through the company's normal device
configuration. Certificates and CA roots are never committed. See
[Java container trust](../../docker/README.md) for certificate format and restart
requirements.

## First Installation

Core initializes an empty metadata database through Flyway. When replacing an
older metadata schema, read the [Flyway guide](../../core/docs/flyway.md) first:
measurement records refer to metadata UUIDs, so a destructive reset of metadata
also requires a coordinated measurement reset unless an ID-preserving migration
is planned. **Do not reset existing volumes as a routine deployment step.**
Back up data that must be retained before any migration or deliberate reset.

1. Prepare configuration, secrets, TLS, and trust as described above.
2. Validate the intended Core image tag without pulling images or changing
   services. Replace the example tag with an image that has been published:

```sh
deploy/single-host/scripts/deploy.sh --check sha-42bd19b
```

The check validates selected environment constraints and Compose structure. It
does not verify credentials, registry availability, DNS, live TLS, or application
readiness.

3. For a **new or deliberately emptied Keycloak database**, run the offline
   bootstrap while Keycloak is stopped:

```sh
deploy/single-host/scripts/bootstrap-keycloak.sh
```

The bootstrap starts the Keycloak database, imports the realm only when absent,
and starts Keycloak. It refuses to run while Keycloak is active and never resets
the database. The seed contains the API and browser clients, roles, and the
monitoring group, but no browser users or connector service clients. Routine
deployment does not import or update realm settings.

4. Deploy the platform using the validated tag:

```sh
deploy/single-host/scripts/deploy.sh sha-42bd19b
```

This pulls platform images, starts the stack, runs smoke checks, and records
the Core tag after success. The frontend hostname can return `503` until its
separate release is installed.

5. Deploy the frontend by its published digest. Replace the placeholder in this
   quoted reference with the actual 64-character lowercase SHA-256 digest:

```sh
deploy/single-host/scripts/deploy-frontend.sh \
  'ghcr.io/viadonau/pegelhub-frontend@sha256:<64-lowercase-hex>'
```

6. Onboard browser users below. Enroll and configure connectors separately using
   the [connector runner guide](../connector/README.md).

## Updates, Rollback, And Health

For a platform update, synchronize missing environment keys, review newly added
values, validate the desired image tag, and run `deploy.sh` with that tag. The
script preserves the separately managed frontend. It records release tags in
`$PEGELHUB_STATE_DIR/current-release.env`; it does not update the image tag in
`pegelhub.env`.

To force a Keycloak restart after changing its mounted login theme:

```sh
deploy/single-host/scripts/deploy.sh --refresh-keycloak sha-42bd19b
```

This recreates Keycloak without importing or overwriting realm settings. A
normal deployment does not force its recreation.

Backend and frontend rollbacks are separate operations:

| Target | Command | Scope |
| --- | --- | --- |
| Core | `deploy/single-host/scripts/deploy.sh --rollback` | Deploys the previous successfully recorded Core tag and reruns platform smoke checks. |
| Frontend | `deploy/single-host/scripts/deploy-frontend.sh --rollback` | Restores the previous digest recorded in `frontend-release.env`. |

These commands require a recorded previous release. They do not restore
database contents, realm settings, checkout files, or configuration. Backend
deployment does not automatically roll back on failure. Frontend deployment
attempts to restore the last successful frontend release if activation or smoke
checks fail; a failed first frontend release is removed.

Run the platform checks independently:

```sh
deploy/single-host/scripts/smoke.sh
```

The smoke script checks TLS for all three hostnames, the public API system-time
route, Keycloak issuer discovery, and internal Core and Keycloak health. It
checks the frontend page only when a frontend container is running. It does
not test user sign-in or connector ingestion end to end.

## Browser User Onboarding And Recovery

PegelHub uses Keycloak temporary passwords for user onboarding and
administrator-assisted recovery. The committed deployment does not configure
SMTP. Keep **Forgot password** disabled until an approved SMTP route is
configured; without it, Keycloak cannot send reset links.

In the `pegelhub` realm of the Keycloak Admin Console:

1. Create an enabled user with a username, first name, last name, and email.
2. Open the user's **Groups** tab and join `/monitoring-users`.
3. Open **Credentials**, set a random password of at least 12 characters, and
   leave **Temporary** enabled.
4. Deliver the username and temporary password through the approved company
   channel. Never put credentials in source control, tickets, or deployment
   configuration.
5. Ask the user to open PegelHub and sign in. Keycloak requires a new password
   before returning the browser to the application.
6. Confirm activation within 24 hours. If **Update Password** is still listed
   under the user's required actions, the temporary password is unused: disable
   the account, then enable it and issue a new temporary password only when the
   user is ready to complete onboarding.

Keycloak's **Temporary** switch forces a password change at first login; it does
not give the temporary credential an automatic expiry time. The 24-hour window
is therefore an operator-owned control.

For account recovery, set a new temporary password and repeat the same delivery
process. If brute-force protection has temporarily locked the account, clear
that user's login failures in the Admin Console before delivery. The
password-change page logs out other Keycloak sessions and invalidates their
refresh tokens by default. Already issued Core API access tokens are stateless
and can remain valid for their remaining lifetime. The committed realm seed
sets that lifetime to 10 minutes; verify it separately on existing realms.
Do not set a permanent password on a user's behalf. Treat suspected account
compromise as an incident rather than ordinary forgotten-password recovery.

The committed realm seed applies the following policy only to new realms:

- `/monitoring-users` grants `metadata:read` and `measurement:read` from
  `pegelhub-core-api`; it is intentionally not a default group.
- Passwords require at least 12 characters and must differ from the username and
  email address.
- Brute-force protection temporarily locks an account after 10 failures, adds
  one minute per threshold crossing, caps the wait at 15 minutes, and resets the
  failure count after 12 hours. The strategy is `MULTIPLE`, the quick-login
  threshold is 1,000 milliseconds, the minimum quick-login wait is one minute,
  and permanent lockout is disabled.
- Self-service password reset is disabled until an approved SMTP route exists.

Realm import never overwrites an existing realm. For an existing
installation, configure those settings once in the Admin Console, add current
browser users to `/monitoring-users`, and then remove their duplicate direct
`metadata:read` and `measurement:read` mappings after verifying their effective
roles. Under **Authentication** > **Required actions**, verify that **Update
Password** is enabled but is not a default action. Under **Realm settings** >
**User registration** > **Default groups**, verify that `/monitoring-users` is
absent. Existing passwords remain valid until they are next changed. Do not
rerun the offline bootstrap as an update mechanism.
