# Connector Library

This module provides the shared Pegelhub HTTP client and connector runtime seam used by protocol connectors.

## Client Creation

Create Core clients from explicit connection data:

```java
PegelHubClient client = PegelHubClientFactory.http().create(
        new CoreConnection(
                URI.create("http://localhost:8080/").toURL(),
                new CoreAuthentication(
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

Connector modules implement `ConnectorModule` and create a `ConnectorRuntimeDefinition` through
`ConnectorRuntimeAssembly`. The assembly owns startup hooks, fixed-delay tasks, resources, thread count,
and shutdown timeout. Its `AutoCloseable` scope unwinds acquired resources if definition construction fails;
after `complete()`, ownership transfers to `ConnectorRuntime`.

```java
@Override
public ConnectorRuntimeDefinition define(
        ConnectorConfigDirectory configDirectory,
        PegelHubClientFactory coreClients) throws Exception {
    ProtocolConfig config = configLoader.load(configDirectory);
    try (ConnectorRuntimeAssembly runtime = ConnectorRuntimeAssembly.begin(name())) {
        PegelHubClient core = runtime.own(coreClients.create(config.coreConnection()));
        runtime.fixedDelayTask("protocol-sync", new ProtocolSynchronizer(core), config.pollingInterval());
        return runtime.complete();
    }
}
```

`ConnectorRuntime` remains the low-level scheduler and lifecycle owner. Connector entrypoints should not
construct it directly.

## Configuration Helpers

`ConnectorApplication` resolves the config directory from the first CLI argument, defaulting to `/app/config`,
and creates the production `PegelHubClientFactory`. It passes both dependencies explicitly to the connector module.

`ConnectorConfigDirectory` provides:

- `resolve(...)` for config-relative paths
- `readYaml(...)` for typed YAML loading
- `listYamlFiles(...)` for sorted mapping files

Shared `PollingConfig.duration()` parses `30s`, `15m`, and `1h` style values. `ConnectorMappingLoader`
loads mapping files in deterministic filename order and enforces required cardinality and directions.

Connector-specific config loaders depend only on `ConnectorConfigDirectory`. They isolate YAML schema parsing,
mapping validation, and conversion to normalized runtime config from resource and task assembly.

## Time Handling

Measurement timestamps use `Instant`. Outbound HTTP payloads use ISO-8601 UTC strings such as:

```json
{
  "observedAt": "2026-04-25T10:15:30Z"
}
```

Connectors that receive protocol timestamps without an explicit offset must choose the timezone at the parsing boundary.
