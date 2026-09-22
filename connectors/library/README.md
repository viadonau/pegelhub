# Connector Library

This module is the shared runtime for the protocol connectors. It owns the Core
HTTP client, client-credentials token handling, YAML configuration access,
mapping conventions, polling configuration, and connector lifecycle. It is a
library, not a standalone process.

## Build and Test

Use JDK 21 and Maven 3.9. From the repository root:

```bash
mvn -B -ntp -pl connectors/library -am test
```

## Core Client

Create a client from explicit connection data and close it when finished:

```java
try (PegelHubClient client = PegelHubClientFactory.http().create(
        new CoreConnection(
                URI.create("http://localhost:8080/").toURL(),
                new CoreAuthentication(
                        "http://pegelhub-keycloak.test:8082/realms/pegelhub/protocol/openid-connect/token",
                        "local-connector-example",
                        "<client-secret>")))) {
    // Read or write measurements through client.
}
```

The HTTP client obtains a short-lived token with the OAuth 2.0
`client_credentials` grant, caches it until refresh is needed, and sends it as
a bearer token. Core validates the token issuer, the `pegelhub-core-api`
audience, and lowercase client roles such as `measurement:write`.

Set `baseUrl` to the Core application root, not `/api/v1`. End a path-prefixed
base URL with `/`: the client resolves its API routes relative to that URL.

Default Core and token HTTP requests use 10-second connection and
pool-acquisition timeouts and a 30-second response timeout. The factory also
accepts `CoreClientOptions` for callers needing different timeouts or strict
latest-response validation. Automatic HTTP retries are disabled; connector jobs
decide what to retain or retry.

Measurement reads have two forms:

- Range reads use half-open `[from, to)` windows and ascending order. Truncated
  responses are split into smaller intervals; a failed child request or an
  indivisible truncated interval fails the whole read, not just part of it.
- Latest-value reads search the last 365 days. An empty result does not prove
  that the time series has no older data.

Every read checks the time-series ID, requested representation, and response
unit. Values are already converted by Core; do not convert them again.
Writes submit values unchanged and let Core apply the target source assignment's
input representation. See [measurement representations](../../docs/guides/measurement-representations.md).

## Application Entry Point

Connector mains delegate startup to the shared application:

```java
ConnectorApplication.run(args, new MyConnectorModule());
```

`ConnectorApplication` uses the first argument as the configuration directory
and defaults to `/app/config`. It creates the HTTP Core client factory and
passes both dependencies to the connector module.

A module implements `ConnectorModule` and returns a complete runtime
definition:

```java
@Override
public ConnectorRuntimeDefinition define(
        ConnectorConfigDirectory configDirectory,
        PegelHubClientFactory coreClients) throws Exception {
    ProtocolConfig config = configLoader.load(configDirectory);
    try (ConnectorRuntimeAssembly runtime = ConnectorRuntimeAssembly.begin(name())) {
        PegelHubClient core = runtime.own(coreClients.create(config.coreConnection()));
        runtime.fixedDelayTask(
                "protocol-sync",
                new ProtocolSynchronizer(core),
                config.pollingInterval());
        return runtime.complete();
    }
}
```

The assembly owns startup hooks, scheduled tasks, resources, thread count, and
shutdown timeout. Its `AutoCloseable` scope releases resources when definition
construction fails; after `complete()`, ownership transfers to
`ConnectorRuntime`. Entrypoints should not construct the low-level runtime
directly.

Tasks use fixed delay: the next run starts after the previous run finishes plus
the configured delay. The assembly defaults to one worker, no initial delay,
and a 15-second shutdown wait before interrupting workers, followed by another
wait of up to 15 seconds. Modules can override these defaults. An ordinary task
exception is logged without cancelling future runs. The runtime is not a
durable queue and does not detect stalled workers or persist retry state.

## Configuration Conventions

`ConnectorConfigDirectory` resolves paths against the selected configuration
root, reads typed YAML, and lists mapping YAML files in sorted filename order.
Only regular `.yaml` and `.yml` files directly inside the mapping directory are
loaded; nested directories are not scanned. Connector-specific loaders own
their YAML schema and mapping validation. Unknown YAML fields fail loading.
Configuration is read at startup, without environment-variable interpolation
or live reload. Restart the process after changing it.

Shared conventions are:

- main configuration file: `connector.yaml`
- default mapping directory: `mappings`
- polling interval: a required positive integer followed by `s`, `m`, or `h`
  (suffixes are case-insensitive)
- directions: `external-to-core` and `core-to-external`
- timestamps: `Instant`, serialized as ISO-8601 UTC such as
  `2026-04-25T10:15:30Z`

Protocol-specific parsers decide how an offset-free timestamp becomes an
`Instant`; this is not normalized by the shared library. Verify those semantics
before deployment. For example, the FTP ZRXP parser uses UTC while its ASC
parser uses the connector JVM's default timezone. Consult the connector-specific
README for configuration fields, mapping cardinality, and supported directions.

## Core Authorization Prerequisites

A role-bearing token is necessary but not sufficient for connector access.
Measurement clients need `pegelhub_actor_type: CLIENT` and a client ID in `azp`
or `client_id`. Core measurement policies also require that client ID to match
an active Connector record. Measurement writes require that Connector to be
the time series' source assignment and the complete metadata hierarchy to be
active. Measurement reads by every connector client require a covering station
or time-series read-access relation. Connector clients must remain registered
and active even when their token also carries `system:admin`; that authority
is an operator-user capability, not a connector bypass.
The [Bruno write workflow](../../core/docs/api/bruno/#write-workflow) shows the
registration and metadata sequence.

The bundled protocol connectors exchange measurements only. They do not
register metadata automatically, submit telemetry, or expose an HTTP health
endpoint. Logs go to standard output; `LOG_LEVEL` defaults to `INFO`. Successful
startup alone does not demonstrate that a protocol transfer or Core write works.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Token request fails | Token URL reachability, confidential-client settings, client ID, and secret |
| Core returns `401` | Exact issuer match and the `pegelhub-core-api` token audience |
| Core returns `403` | Required lowercase role, active Connector, active metadata hierarchy, exact source binding, or an applicable read-access relation |
| Core reports connector not registered | Connector metadata whose `keycloakClientId` matches token `azp`, or `client_id` when `azp` is absent |
| Connector exits during startup | `connector.yaml`, mapping count/directions, UUIDs, and polling interval syntax |
| Container cannot reach `localhost` endpoints | Use host-reachable names, `host.docker.internal` on Docker Desktop, or service names on a shared Docker network |
| Protocol cycle fails | Protocol endpoint reachability and connector logs for the affected mapping |

## Connector Guides

- [FTP](../ftp-connector/)
- [ICC](../icc-connector/)
- [IEC 60870-5-104](../iec-connector/)
- [mA / Revolution Pi](../ma-connector/)
- [TSTP](../tstp-connector/)
