# Library

This module provides the plain Java API client used by the connectors.

## Usage

Create a communicator with:

- `baseUrl`: HTTP(S) base URL of the Pegelhub cluster
- `propertiesFile`: optional YAML file path, defaults to `pegelhub.yaml`
- `...Route`: optional route overrides for advanced use

The checked-in sample file lives at `examples/config/pegelhub.yaml`.

The YAML contains the connector identity and authentication data:

- `keycloak.tokenUrl`
- `keycloak.clientId`
- `keycloak.clientSecret`
- `sendMetaDataOnStartup`
- `isSupplier`
- `supplier`
- `taker`

Keycloak clients must be pre-provisioned per connector instance. The `supplier.id` / `taker.id` and connector numbers must also be unique within the target Pegelhub cluster.
When omitted, `sendMetaDataOnStartup` defaults to `false`, so normal connector credentials only send measurement or telemetry data after an operator has registered the metadata through the Pegelhub admin API. Set it to `true` only for admin-capable credentials that are allowed to create supplier or taker metadata during startup.

## Authentication

- Ask the Pegelhub owner for a Keycloak client id and client secret with the required roles.
- Configure `keycloak.tokenUrl`, `keycloak.clientId`, and `keycloak.clientSecret` in `pegelhub.yaml`.
- The library obtains a short-lived access token with `client_credentials`, caches it, and sends `Authorization: Bearer <token>`.

The library does not write access tokens or secrets back to the YAML file.

## Time Handling

Measurement and telemetry timestamps are represented as `Instant` in the connector library.
Outbound HTTP payloads therefore use ISO-8601 UTC strings such as:

```json
{
  "timestamp": "2026-04-25T10:15:30Z"
}
```

Connectors that receive protocol timestamps without an explicit offset must choose the timezone at the parsing boundary.
For Pegelhub's current connectors, offset-free protocol timestamps are treated as UTC before they are stored in the shared `Measurement` model.
The HTTP client expects timestamp values from Core to use the same instant format.
