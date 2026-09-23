# Core API Bruno Collection

This directory is a repository-owned [Bruno](https://www.usebruno.com/)
collection for exercising the PegelHub Core API. The generated English OpenAPI
document at `/v3/api-docs?lang=en` remains the authoritative HTTP contract.

## Requirements

- Bruno Desktop or the `bru` CLI 3.0 or newer, with
  [OpenCollection YAML support](https://docs.usebruno.com/opencollection-yaml/overview)
- reachable Core and Keycloak endpoints
- a hosts entry for `pegelhub-keycloak.test` when using the local environment

Open this directory, containing [`opencollection.yml`](opencollection.yml), as
the collection root and select an environment in Bruno. Do not open an
individual YAML request as a collection. CLI commands below run from this
directory; from the repository root, first run `cd core/docs/api/bruno`.

## Local read-only run

Start the [Core local stack](../../../README.md#local-docker-stack), select `Local`, and
try `Measurements/Get System Time`. The local environment uses only the
disposable clients and secrets imported by the checked-in development realm.

From this directory, run all requests tagged `read-only`:

```bash
bru run -r --tags=read-only --env Local --bail
```

This selects the tagged metadata-list requests, telemetry range query, and
public system-time request. It works against an empty application database and
does not create, update, or delete application data, although protected
requests obtain tokens from Keycloak. Not every GET request is tagged:
individual-resource and measurement queries need IDs first.

Do not use an unfiltered recursive run as a smoke test. The collection also
contains create/update requests and access-grant revocations, and its folder
order is not a complete provisioning workflow.

## Another environment

Create one ignored environment per target:

```bash
cp environments/Remote.example.yml environments/Staging.local.yml
```

Set `name: Staging.local` to match the new filename and configure:

| Variable | Value |
| --- | --- |
| `baseUrl` | Core origin without a trailing slash |
| `apiPath` | API path without surrounding slashes, normally `api/v1` |
| `keycloakTokenUrl` | Complete OpenID Connect token endpoint |
| `operatorClientId`, `operatorClientSecret` | Operator service account |
| `connectorClientId`, `connectorClientSecret` | Connector service account |

For the full workflow, the operator client must support client credentials,
have the `pegelhub-core-api` client role `system:admin`, and receive
`pegelhub_actor_type: USER`. The connector client used by the
collection's write requests needs `measurement:write` and `telemetry:write`
and receives `pegelhub_actor_type: CLIENT`. Both tokens need the configured
issuer and the `pegelhub-core-api` audience. The connector registration
request binds `connectorClientId` to Connector metadata in Core; it does not
create a Keycloak client or assign Keycloak roles.

Run the read-only requests with the filename minus `.yml`:

```bash
bru run -r --tags=read-only --env Staging.local --bail
```

The `*.example.yml` file is a template, not a runnable target. Files matching
`environments/*.local.yml` are ignored, but they contain
credentials as plain text. Keep them private and never commit them.

## Authentication profiles

Each request chooses `operator`, `connector`, or `none` with the collection's
internal `X-Auth-Profile` header. The before-request script removes that header,
obtains a client-credentials token, caches it by token endpoint and client ID,
and sends only the bearer token to Core.

## Write workflow

Use only a disposable local environment or a target where you deliberately
intend to create data. Send the following requests in order within the same
Bruno session so their response scripts can carry generated IDs forward:

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

You can then run `Monitoring/List Monitoring Time Series` and
`Monitoring/Get Monitoring Time Series` to inspect the same dataset through
the frontend's read contract. For technical telemetry, send
`Telemetry/Write Telemetry` followed by `Telemetry/Get Latest Telemetry`;
latest telemetry is keyed by the registered Connector UUID, not TimeSeries UUID.

The station read grant in step 7 is separate from the source assignment created
in step 6: source ownership authorizes writes, while read grants authorize
connector reads. The supplied measurement-read requests use the operator
profile, so they do not test the connector's read permissions. To read as a
connector, it also needs the `measurement:read` role and a matching read grant.

Registration accepts an existing identity only when Core reports the matching
client-ID conflict; the list request then captures that Connector's ID. It does
not reactivate an inactive connector. Subsequent create requests make new
metadata records, and writes persist in InfluxDB. There is no automatic cleanup
or transaction rollback for this workflow.

## Contract coverage

The running Core application generates the authoritative English OpenAPI
document at `/v3/api-docs?lang=en`. The collection covers metadata, the observed
property catalog, connector identity/access operations, monitoring reads,
measurements, and telemetry. It is a maintained set of examples, not generated
code or exhaustive schema coverage. Request scripts check selected response
conditions; a successful run is not a complete authorization or integration
test suite.
