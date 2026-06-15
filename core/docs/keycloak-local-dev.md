# Local Keycloak Development

PegelHub local development uses a disposable Keycloak realm so Core and connectors can exercise OAuth2 client credentials without depending on production identity infrastructure.

## Stable Local Hostname

Use this issuer everywhere in local development:

```text
http://pegelhub-keycloak.test:8082/realms/pegelhub
```

That hostname is configured as a Docker Compose network alias for the Keycloak container. Add a local hosts entry so host-side tools resolve the same name to the exposed local port:

```text
127.0.0.1 pegelhub-keycloak.test
```

Do not request tokens through `localhost` while Core validates `pegelhub-keycloak.test`; the JWT `iss` claim must exactly match Core's `KEYCLOAK_ISSUER_URI`.

## Start Local Keycloak

Copy `core/.env.example` to `core/.env` if needed, then start the local stack:

```sh
bash .agents/skills/pegelhub-local-dev/scripts/pegelhub-local-dev.sh compose-up
```

The compose stack adds:

- `keycloak-db`, a dedicated local Postgres database for Keycloak;
- `keycloak`, exposed at `http://pegelhub-keycloak.test:8082`;
- realm import from `core/docker/keycloak/import/pegelhub-realm.json`.

Realm import runs only when the realm does not already exist. If you need to recreate the local realm, stop and remove only the Keycloak database volume after explicitly accepting local identity data loss.

## Local Realm Contents

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

These are throwaway local credentials. Never reuse them outside local development.

## Request A Connector Token

Use the connector client credentials:

```sh
curl -s \
  -d grant_type=client_credentials \
  -d client_id=local-connector-example \
  -d client_secret=local-dev-connector-secret-change-me \
  http://pegelhub-keycloak.test:8082/realms/pegelhub/protocol/openid-connect/token
```

The response contains an `access_token`. Do not paste real tokens into docs, commits, or issue trackers.

## Inspect A Token Without Printing Secrets

For local debugging, decode only the JWT payload:

```sh
TOKEN="<access token>"
printf '%s' "$TOKEN" | cut -d. -f2 | base64 --decode 2>/dev/null
```

Expected local connector token claims:

- `iss` is `http://pegelhub-keycloak.test:8082/realms/pegelhub`;
- `aud` contains `pegelhub-core-api`;
- `azp` is `local-connector-example`;
- `resource_access.pegelhub-core-api.roles` contains `measurement:write` and `telemetry:write`.

## Core Configuration

Core receives these environment variables from Docker Compose:

```text
KEYCLOAK_ISSUER_URI=http://pegelhub-keycloak.test:8082/realms/pegelhub
```

Spring Security must not disable issuer or audience validation to make local development easier. If token validation fails, first compare the token `iss` claim with `KEYCLOAK_ISSUER_URI` and verify that the token `aud` claim contains the fixed API audience `pegelhub-core-api`.

## First-Slice Revocation Rule

Core validates JWTs offline. Disabling a Keycloak client or rotating its secret stops future token issuance, but already issued access tokens remain usable until they expire. The local realm uses a short access-token lifetime to match the first-slice production expectation.

## Legacy Token Schema Cleanup

The app-managed PegelHub API token code path has been removed. Existing local or production databases may still contain the old token table or connector token column until explicit schema migration tooling is introduced. Treat those columns as unused legacy data; do not rely on Hibernate auto-update for destructive cleanup.
