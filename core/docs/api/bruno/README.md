# PegelHub Core API Bruno collection

This collection contains runnable examples for the PegelHub Core API. The
English OpenAPI document at `/v3/api-docs?lang=en` remains the authoritative
HTTP contract.

## Requirements

- [Bruno Desktop or CLI 3.0 or newer](https://docs.usebruno.com/opencollection-yaml/overview);
  the collection is verified with CLI 4.0.
- A reachable PegelHub Core and Keycloak realm.

Open this directory as a collection, not an individual YAML file. Select the
active environment from Bruno's environment menu in the top-right corner.

## Local

`environments/Local.yml` matches the repository's local stack. Follow the
[Core local-development guide](../../../README.md#local-development), then open
the collection and select `Local`. A harmless first request is
`Measurements/Get System Time`.

To run every read-only request, execute this from the collection directory:

```shell
bru run -r --tags=read-only --env Local --bail
```

The smoke run may be used against an empty database and never creates, updates,
or deletes data.

## Remote Hosts

Create one ignored environment file per host:

```shell
cp environments/Remote.example.yml environments/Staging.local.yml
```

Edit the copy and give it a distinct `name`, such as `Staging`. Configure:

| Variable | Value |
| --- | --- |
| `baseUrl` | Core base URL without a trailing slash |
| `apiPath` | API path without surrounding slashes, normally `api/v1` |
| `keycloakTokenUrl` | Complete OpenID Connect token endpoint |
| `operatorClientId` / `operatorClientSecret` | Operator service-account credentials |
| `connectorClientId` / `connectorClientSecret` | Connector service-account credentials |

The operator client must be allowed to use client credentials and have
`system:admin`. The connector client must have `measurement:write` and
`telemetry:write`. Tokens must use the issuer configured by Core and include the
`pegelhub-core-api` audience. The registration workflow binds the configured
connector client ID to its Connector metadata.

Select the new environment in Bruno. For the CLI, use its filename without
`.yml`:

```shell
bru run -r --tags=read-only --env Staging.local --bail
```

Files ending in `.local.yml` are ignored by Git but contain credentials as
plain text. Keep them private and never commit them. Repeat the copy step with a
different filename and `name` for each additional host.

## Authentication

Each request selects `operator`, `connector`, or `none` through an internal
`X-Auth-Profile` header. The collection removes this header before sending the
request, obtains the appropriate client-credentials token, and caches it only
for the matching token endpoint and client ID. Tokens do not need to be copied
into environment files.

## Metadata-To-Measurement Workflow

**This workflow writes persistent data to the selected environment.** Run these
requests in order:

1. `Connector Registration/Register Connector Identity`
2. `Connectors/List Connectors`
3. `Station Owners/Create Station Owner`
4. `Stations/Create Station`
5. `Measuring Points/Create Measuring Point`
6. `Time Series/Create Time Series`
7. `Access Grants/Create Access Grant`
8. `Measurements/Write Measurements`
9. `Measurements/Read Raw Measurements`
10. `Measurements/Read Measurement Buckets`

Response scripts carry generated IDs between requests. Registration may be run
again: only the specific "connector already exists" response is accepted, and
the following list request verifies and captures that Connector.

## Contract Coverage

`OpenApiDocumentationWebMvcTest#brunoCollectionMatchesEnglishOpenApiOperationsAndQueryParameters`
compares the collection's operations and query options with the generated
English OpenAPI document and rejects missing, stale, or duplicate entries.
