# TSTP Connector

This connector reads from or writes to a TSTP server and exchanges measurements with Pegelhub Core.

## Build

```sh
mvn -pl connectors/tstp-connector -am -DskipTests package
```

## Configuration

The connector accepts an optional first CLI argument pointing to the config directory. Without an argument it reads from `/app/config`.

The config directory must contain `connector.yaml` and a `mappings/` directory. Phase 2 requires exactly one mapping file.

`connector.yaml`:

```yaml
core:
  baseUrl: "http://localhost:8080/"
  authentication:
    tokenUrl: "http://localhost:8082/realms/pegelhub/protocol/openid-connect/token"
    clientId: "connector"
    clientSecret: "secret"
polling:
  interval: "30s"
mappings:
  directory: "mappings"
tstp:
  server:
    host: "127.0.0.1"
    port: 8030
```

`mappings/station.yaml`:

```yaml
timeSeriesId: "11111111-1111-1111-1111-111111111111"
stationId: 123
direction: "external-to-core"
```

Use `direction: "external-to-core"` to read from TSTP into Core. Use `direction: "core-to-external"` to write Core measurements to TSTP.

## Docker

```sh
scripts/build-connector-image.sh tstp-connector

docker run --rm -d \
  -v "$(pwd)/examples/config:/app/config:ro" \
  pegelhub-tstp-connector:local
```
