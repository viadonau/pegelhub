# Single-host deployment

Reusable Docker Compose deployment for one PegelHub host. Host-specific
settings live outside the Git checkout:

```text
/etc/pegelhub/<deployment>/
  pegelhub.env
  tls/server/
  tls/trust/

/var/lib/pegelhub/<deployment>/state/
```

Set these paths before running an operational script:

```sh
export PEGELHUB_CONFIG_DIR=/etc/pegelhub/<deployment>
export PEGELHUB_STATE_DIR=/var/lib/pegelhub/<deployment>/state
```

Copy [`pegelhub.env.example`](pegelhub.env.example) into the configuration
directory as `pegelhub.env`, choose a unique `COMPOSE_PROJECT_NAME`, and replace
the example hostnames and secrets. Staging keeps
`COMPOSE_PROJECT_NAME=pegelhub-staging` so existing named volumes are reused.

## TLS and trust

| Installation | `PEGELHUB_TLS_MODE` | `PEGELHUB_TRUST_MODE` |
| --- | --- | --- |
| Public/default | `automatic` | `system` |
| Provided certificate | `provided` | `system` |
| Provided certificate and private CA | `provided` | `custom` |

`automatic` uses Caddy's built-in ACME support. `provided` loads PEM bundles
from `tls/server/current`. Install one shared SAN pair or multiple named pairs:

```sh
deploy/single-host/scripts/install-certificates.sh \
  /private/incoming/shared.fullchain.pem \
  /private/incoming/shared.privkey.pem
```

The installer checks expiration, hostname coverage, and matching private keys,
replaces the current bundles, and reloads Caddy when it is already running.

`custom` adds the `*.crt` files in the platform's `tls/trust` directory to the
Core Java truststore. Each independently deployed connector uses its own
`trust/` directory. Managed browsers must trust the company CA through the
company's normal device configuration. Certificates and CA roots are never
committed.

## Operations

The operational metadata catalog uses a clean Flyway V1 baseline. Before its
first deployment, reset the PostgreSQL metadata and InfluxDB measurement volumes
together. This is irreversible and deliberately requires the Compose project
name as confirmation:

```sh
deploy/single-host/scripts/deploy.sh \
  --reset-data pegelhub-staging sha-<short-sha>
```

The command preserves Caddy, Keycloak, and frontend state. GitHub's `Images`
workflow exposes the same operation only through the manual
`reset_staging_data` input; ordinary push deployments never reset data. See the
[Flyway guide](../../core/docs/flyway.md) for why the V2 reset is required.

Initialize missing environment keys and server-generated secrets:

```sh
deploy/single-host/scripts/sync-env-template.sh
deploy/single-host/scripts/init-env-secrets.sh
```

Validate or deploy a backend image:

```sh
deploy/single-host/scripts/deploy.sh --check sha-<short-sha>
deploy/single-host/scripts/deploy.sh sha-<short-sha>
deploy/single-host/scripts/deploy.sh --rollback
```

Deploy the independently released frontend:

```sh
deploy/single-host/scripts/deploy-frontend.sh \
  ghcr.io/viadonau/pegelhub-frontend@sha256:<64-lowercase-hex>
```

For a new or deliberately emptied Keycloak database, stop Keycloak and run:

```sh
deploy/single-host/scripts/bootstrap-keycloak.sh
```

## Browser user onboarding and recovery

PegelHub uses Keycloak temporary passwords for user onboarding and
administrator-assisted recovery. SMTP is not configured, so **Forgot password**
must remain disabled and Keycloak must not be expected to send reset links.

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
and can remain valid for their remaining lifetime, which is at most 10 minutes.
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

Realm import never overwrites the persistent staging realm. For an existing
installation, configure those settings once in the Admin Console, add current
browser users to `/monitoring-users`, and then remove their duplicate direct
`metadata:read` and `measurement:read` mappings after verifying their effective
roles. Under **Authentication** > **Required actions**, verify that **Update
Password** is enabled but is not a default action. Under **Realm settings** >
**User registration** > **Default groups**, verify that `/monitoring-users` is
absent. Existing passwords remain valid until they are next changed. Do not
rerun the offline bootstrap as an update mechanism.

Deploy this theme change with:

```sh
deploy/single-host/scripts/deploy.sh --refresh-keycloak <image-tag>
```

`--refresh-keycloak` recreates Keycloak so its production theme caches reload;
it preserves the database and does not import or overwrite realm settings. A
normal deployment without that option does not force Keycloak recreation.

Run route and internal health checks independently with:

```sh
deploy/single-host/scripts/smoke.sh
```

Connector workloads use the shared runner in [`../connector`](../connector/)
and are deployed separately from the platform stack.
