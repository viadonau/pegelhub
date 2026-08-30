# Connector library

This module is the shared runtime for the protocol connectors. It owns the Core
HTTP client, client-credentials token handling, YAML configuration access,
mapping conventions, polling configuration, and connector lifecycle. It is a
library, not a standalone process.

## Build

From the repository root:

```bash
mvn -B -ntp -pl connectors/library -am test
```

## Core client

Create clients from explicit connection data:

```java
PegelHubClient client = PegelHubClientFactory.http().create(
        new CoreConnection(
                URI.create("http://localhost:8080/").toURL(),
                new CoreAuthentication(
                        "http://pegelhub-keycloak.test:8082/realms/pegelhub/protocol/openid-connect/token",
                        "local-connector-example",
                        "<client-secret>")));
```

The HTTP client obtains a short-lived token with the OAuth 2.0
`client_credentials` grant, caches it until refresh is needed, and sends it as
a bearer token. Core still validates issuer, the `pegelhub-core-api` audience,
and lowercase client roles such as `measurement:write`.

Window reads require an inclusive `from`, an exclusive `to`, and `to > from`.
They request at most 10,000 ascending points at a time and bisect a truncated
window until the explicit interval is complete. An indivisible window that
still exceeds the limit fails rather than silently dropping points. Mismatched
responses also fail. Latest-value reads use a fixed 365-day window. Every
outgoing measurement must provide a time-series ID, observation timestamp, and
value. Client creation configures a 10-second connection-request timeout, a
10-second connect timeout, and a 30-second response timeout.

## Application entry point

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

## Configuration conventions

`ConnectorConfigDirectory` resolves paths against the selected configuration
root, reads typed YAML, and lists mapping YAML files in sorted filename order.
Connector-specific loaders are responsible for their YAML schema and mapping
validation.

Shared conventions are:

- main configuration file: `connector.yaml`
- default mapping directory: `mappings`
- polling interval: a positive integer followed by `s`, `m`, or `h`
  (case-insensitive)
- directions: `external-to-core` and `core-to-external`
- timestamps: `Instant`, serialized as ISO-8601 UTC such as
  `2026-04-25T10:15:30Z`

Protocol-specific parsers decide how an offset-free timestamp becomes an
`Instant`; this is not normalized by the shared library. Verify those semantics
before deployment. For example, the FTP ZRXP parser uses UTC while its ASC
parser uses the connector JVM's default timezone. Consult the connector-specific
README for configuration fields, mapping cardinality, and supported directions.

## Core authorization prerequisites

A role-bearing token is necessary but not sufficient for connector access.
Measurement clients need `pegelhub_actor_type: CLIENT` and a client ID in `azp`
or `client_id`. Core measurement policies also require that client ID to match
an active Connector record. Measurement writes require that Connector to be
the time series' source assignment and the complete metadata hierarchy to be
active. Measurement reads by every connector client require a covering station
or TimeSeries read-access relation. Connector clients must remain registered
and active even when their token also carries `system:admin`; that authority
is an operator-user capability, not a connector bypass.
The [Bruno write workflow](../../core/docs/api/bruno/#write-workflow) shows the
registration and metadata sequence.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Token request fails | Token URL reachability, confidential-client settings, client ID, and secret |
| Core returns `401` | Exact issuer match and the `pegelhub-core-api` token audience |
| Core returns `403` | Required lowercase role, active Connector, active metadata hierarchy, exact source binding, or an applicable read-access relation |
| Core reports connector not registered | Connector metadata whose `keycloakClientId` matches token `azp` |
| Connector exits during startup | `connector.yaml`, mapping count/directions, UUIDs, and polling interval syntax |
| Container cannot reach `localhost` endpoints | Use host-reachable names, `host.docker.internal` on Docker Desktop, or service names on a shared Docker network |
| Protocol cycle fails | Protocol endpoint reachability and connector logs for the affected mapping |

## Connector guides

- [FTP](../ftp-connector/)
- [ICC](../icc-connector/)
- [IEC 60870-5-104](../iec-connector/)
- [mA / Revolution Pi](../ma-connector/)
- [TSTP](../tstp-connector/)
