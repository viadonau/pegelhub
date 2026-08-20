# Local Keycloak development

PegelHub local development uses a disposable Keycloak realm for browser login
and OAuth 2.0 service-client authentication. It does not depend on shared or
production identity infrastructure.

## Stable local hostname

Use this issuer everywhere in local development:

```text
http://pegelhub-keycloak.test:8082/realms/pegelhub
```

That hostname is configured as a Docker Compose network alias for the Keycloak
container. Add a local hosts entry so host-side tools resolve the same name to
the exposed local port:

```text
127.0.0.1 pegelhub-keycloak.test
```

Do not request tokens through `localhost` while Core validates
`pegelhub-keycloak.test`; the JWT `iss` claim must exactly match Core's
`KEYCLOAK_ISSUER_URI`.

## Start local Keycloak

Copy `core/.env.example` to `core/.env` if needed, then start the local stack:

```bash
test -f core/.env || cp core/.env.example core/.env
scripts/local-stack.sh compose-up
```

The compose stack adds:

- `keycloak-db`, a dedicated local Postgres database for Keycloak
- `keycloak`, exposed at `http://pegelhub-keycloak.test:8082`
- realm import from `core/docker/keycloak/import/pegelhub-realm.json`
- a bind-mounted local login theme from `core/docker/keycloak/themes` to
  `/opt/keycloak/themes`

`PEGELHUB_FRONTEND_URL` defines the frontend origin used by the imported Keycloak
client for redirects, web origins, and the login error page's **Back to
Application** link. Set it independently in each environment. Realm import runs
only when the realm does not already exist, so changing this value does not
update an existing realm.

## Iterate on the login theme

The local Keycloak container runs in `start-dev` mode with theme caches
disabled:

```text
--spi-theme-static-max-age=-1
--spi-theme-cache-themes=false
--spi-theme-cache-templates=false
```

Edit the theme files under:

```text
core/docker/keycloak/themes/pegelhub/login
```

Then reload the browser tab that shows the Keycloak login page. Ordinary CSS
and FreeMarker template changes do not require an image rebuild or Keycloak
restart. If you change Docker Compose itself, recreate Keycloak once so the new
command flags are applied:

```bash
docker compose --env-file core/.env -f core/docker-compose.yaml \
  up -d --force-recreate keycloak
```

Realm import runs only when the realm does not already exist. Recreating the
local realm requires stopping the stack and removing the Keycloak database
volume after explicitly accepting local identity data loss.

## Local realm contents

Realm:

```text
pegelhub
```

Resource/API client:

```text
pegelhub-core-api
```

Initial roles:

```text
measurement:write
measurement:read
telemetry:write
telemetry:read
metadata:write
metadata:read
system:admin
```

Local-only client-credentials clients:

| Client id | Secret | Purpose |
| --- | --- | --- |
| `local-connector-example` | `local-dev-connector-secret-change-me` | Connector write smoke tests. |
| `local-ma-connector` | `local-dev-ma-connector-secret-change-me` | Local RevPi mA connector development. |
| `local-operator` | `local-dev-operator-secret-change-me` | Metadata/admin smoke tests. |

These are throwaway local credentials. Never reuse them outside local
development.

Local browser account:

| Username | Password | Client | Core roles |
| --- | --- | --- | --- |
| `pegel` | `pegel` | `pegelhub-frontend` | `metadata:read`, `measurement:read` |

This account and password are checked-in, disposable local fixtures. Never
enable or reuse them in a shared, staging, or production realm.

## Request a connector token

Use the connector client credentials:

```bash
curl -s \
  -d grant_type=client_credentials \
  -d client_id=local-connector-example \
  -d client_secret=local-dev-connector-secret-change-me \
  http://pegelhub-keycloak.test:8082/realms/pegelhub/protocol/openid-connect/token
```

The response contains an `access_token`. Do not paste real tokens into docs,
commits, or issue trackers.

## Verify token claims

Inspect local tokens only with tooling you trust on your machine. Do not paste
real tokens into third-party JWT decoders, documentation, commits, or issue
trackers.

Expected local connector token claims:

- `iss` is `http://pegelhub-keycloak.test:8082/realms/pegelhub`
- `aud` contains `pegelhub-core-api`
- `azp` is `local-connector-example`
- `pegelhub_actor_type` is `CLIENT`
- `resource_access.pegelhub-core-api.roles` contains exactly
  `measurement:write` and `telemetry:write`
- `realm_access` is absent

Service clients do not receive user-profile scopes or the custom user-subject
mapper.

The local operator client uses the user actor scope so that client-credentials
requests made for metadata administration are still treated as operator users:
its `pegelhub_actor_type` claim is `USER` and it has `system:admin`.

The browser client receives `pegelhub_actor_type: USER` and exactly
`metadata:read` plus `measurement:read` under
`resource_access.pegelhub-core-api.roles`. Every caller linked to the Core
audience scope receives `aud: pegelhub-core-api`, independent of its roles.

## Core configuration

Core receives these environment variables from Docker Compose:

```text
KEYCLOAK_ISSUER_URI=http://pegelhub-keycloak.test:8082/realms/pegelhub
```

Do not disable issuer or audience validation for local development. If token
validation fails, compare the token `iss` claim with `KEYCLOAK_ISSUER_URI` and
verify that `aud` contains the fixed API audience `pegelhub-core-api`.

## Token revocation behavior

Core validates signed JWTs locally rather than introspecting them with Keycloak
on every request. Disabling a Keycloak client or rotating its secret stops
future token issuance, but already issued access tokens remain usable until
they expire. The imported local realm sets the access-token lifetime to 600
seconds.
