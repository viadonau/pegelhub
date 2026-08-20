# Core API Bruno collection

This directory is a repository-owned [Bruno](https://www.usebruno.com/)
collection for exercising the PegelHub Core API. The generated English OpenAPI
document at `/v3/api-docs?lang=en` remains the authoritative HTTP contract.

## Requirements

- Bruno Desktop or the `bru` CLI 3.0 or newer; OpenCollection YAML support was
  introduced in 3.0
- reachable Core and Keycloak endpoints
- a hosts entry for `pegelhub-keycloak.test` when using the local environment

Open this directory as the collection root and select an environment in Bruno.
Do not open an individual YAML request as a collection.

## Local read-only run

Start the [Core local stack](../../../#local-docker-stack), select `Local`, and
try `Measurements/Get System Time`. The local environment uses only the
disposable clients and secrets imported by the checked-in development realm.

From this directory, run all requests tagged `read-only`:

```bash
bru run -r --tags=read-only --env Local --bail
```

This run is suitable for an empty database and does not create, update, or
delete application data.

## Another environment

Create one ignored environment per target:

```bash
cp environments/Remote.example.yml environments/Staging.local.yml
```

Set a distinct `name` and configure:

| Variable | Value |
| --- | --- |
| `baseUrl` | Core origin without a trailing slash |
| `apiPath` | API path without surrounding slashes, normally `api/v1` |
| `keycloakTokenUrl` | Complete OpenID Connect token endpoint |
| `operatorClientId`, `operatorClientSecret` | Operator service account |
| `connectorClientId`, `connectorClientSecret` | Connector service account |

The operator client must support client credentials, have `system:admin`, and
receive `pegelhub_actor_type: USER`. The connector client used by the
collection's write requests needs `measurement:write` and `telemetry:write`
and receives `pegelhub_actor_type: CLIENT`. Both tokens need the configured
issuer and the `pegelhub-core-api` audience. The connector registration
request binds `connectorClientId` to Connector metadata in Core.

Run the read-only requests with the filename minus `.yml`:

```bash
bru run -r --tags=read-only --env Staging.local --bail
```

Files matching `environments/*.local.yml` are ignored, but they contain
credentials as plain text. Keep them private and never commit them.

## Authentication profiles

Each request chooses `operator`, `connector`, or `none` with the collection's
internal `X-Auth-Profile` header. The before-request script removes that header,
obtains a client-credentials token, caches it by token endpoint and client ID,
and sends only the bearer token to Core.

## Write workflow

The following sequence writes persistent data to the selected environment:

1. `Connector Registration/Register Connector Identity`
2. `Connectors/List Connectors`
3. `Station Owners/Create Station Owner`
4. `Stations/Create Station`
5. `Measuring Points/Create Measuring Point`
6. `Time Series/Create Time Series`
7. `Connectors/Grant Station Read Access`
8. `Measurements/Write Measurements`
9. `Measurements/Read Raw Measurements`
10. `Measurements/Read Measurement Buckets`

Response scripts carry generated IDs between requests. Registration is
repeatable only for the matching existing connector identity; the following
list request verifies and captures it.

## Contract coverage

The running Core application generates the authoritative English OpenAPI
document at `/v3/api-docs?lang=en`. This collection is a maintained set of
operator and connector smoke-test requests; it is deliberately not generated
code and does not claim exhaustive schema coverage.
