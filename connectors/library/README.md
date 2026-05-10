# Library

This module provides the plain Java API client used by the connectors.

## Usage

Create a communicator with:

- `baseUrl`: HTTP(S) base URL of the Pegelhub cluster
- `propertiesFile`: optional YAML file path, defaults to `pegelhub.yaml`
- `...Route`: optional route overrides for advanced use

The checked-in sample file lives at `examples/config/pegelhub.yaml`.

The YAML contains the connector identity and authentication data:

- `apiToken`
- `lastTokenRefresh`
- `isSupplier`
- `supplier`
- `taker`

`apiToken`s must be unique per connector instance. The `supplier.id` / `taker.id` and connector numbers must also be unique within the target Pegelhub cluster.

## Acquiring A Token

- Create a token under `/api/v1/token?apiKey=<adminToken>&type=<tokenType>`.
- Ask the Pegelhub owner for a token with the required read or write permission.

The library currently refreshes tokens on startup, not continuously during runtime.
