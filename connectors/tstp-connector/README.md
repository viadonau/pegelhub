# TSTP Connector

This connector reads from or writes to a TSTP server and exchanges measurements with Pegelhub Core.

## Build

```sh
mvn -pl connectors/tstp-connector -am -DskipTests package
```

## Configuration

The connector accepts an optional first CLI argument pointing to the config directory. Without an argument it reads from `/app/config`.

The config directory must contain `connector.yaml` and a `mappings/` directory with at least one mapping.
Mapping files are loaded in filename order. One connector process may combine inbound and outbound mappings;
the endpoint, Core client, catalog cache, scheduler, and shutdown lifecycle are shared for the whole process.

`connector.yaml`:

```yaml
core:
  baseUrl: "http://localhost:8080/"
keycloak:
  tokenUrl: "http://localhost:8082/realms/pegelhub/protocol/openid-connect/token"
  clientId: "connector"
  clientSecret: "secret"
schedule:
  delay: "30s"
mappingsDir: "mappings"
tstp:
  address: "127.0.0.1"
  port: 8030
```

`mappings/10-inbound.yaml`:

```yaml
timeSeriesId: "11111111-1111-1111-1111-111111111111"
stationId: 123
direction: "external-to-core"
```

Use `direction: "external-to-core"` to read from TSTP into Core. Use `direction: "core-to-external"` to write Core measurements to TSTP.

The connector continues processing other mappings after an individual failure, logs a cycle summary, and reports
the collected failures after the cycle. Successful reads advance a per-mapping synchronization boundary. Core
lookbacks overlap that boundary by one second and are then filtered to the new logical window, so time spent
processing a cycle does not leave gaps or duplicate the boundary value before the next fixed-delay run. Duplicate
outbound station targets, duplicate inbound Core targets, exact duplicates, and directed feedback cycles are rejected
during startup.

## Docker

```sh
scripts/build-connector-image.sh tstp-connector

docker run --rm -d \
  -v "$(pwd)/examples/config:/app/config:ro" \
  pegelhub-tstp-connector:local
```
