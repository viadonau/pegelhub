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
