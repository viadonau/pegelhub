# Connector Library

This module provides the shared Pegelhub HTTP client and connector runtime seam used by protocol connectors.

## Client Creation

Create Core clients from explicit connection data:

```java
PegelHubClient client = PegelHubClientFactory.create(
        new CoreConnection(
                URI.create("http://localhost:8080/").toURL(),
                new ClientCredentials(
                        "http://localhost:8082/realms/pegelhub/protocol/openid-connect/token",
                        "connector",
                        "secret")));
```

The client obtains short-lived Keycloak tokens with `client_credentials`, caches them, and sends Core requests with a bearer token.

## Connector Runtime

Connector entrypoints should call:

```java
ConnectorApplication.run(args, new MyConnectorModule());
```

Connector modules implement `ConnectorModule` and return a `ConnectorPlan`. The plan describes startup hooks, fixed-delay tasks, close hooks, thread count, and shutdown timeout. `ConnectorRuntime` remains the low-level scheduler behind the plan and should not be built directly by connector entrypoints.

## Configuration Helpers

`ConnectorContext` resolves the config directory from the first CLI argument, defaulting to `/app/config`. It also provides:

- `resolve(...)` for config-relative paths
- `loadYaml(...)` for typed YAML loading
- `listYamlFiles(...)` for sorted mapping files
- `parseDuration(...)` for `30s`, `15m`, and `1h` style values
- `coreClient(...)` for creating a `PegelHubClient` from a `CoreConnection`

Protocol-specific config parsing stays inside connector modules.

## Time Handling

Measurement timestamps use `Instant`. Outbound HTTP payloads use ISO-8601 UTC strings such as:

```json
{
  "observedAt": "2026-04-25T10:15:30Z"
}
```

Connectors that receive protocol timestamps without an explicit offset must choose the timezone at the parsing boundary.
