# ICC Connector

This connector synchronizes selected time-series measurements between two Pegelhub clusters.

## Build

```sh
mvn -pl connectors/icc-connector -am -DskipTests package
```

## Configuration

The connector accepts an optional first CLI argument pointing to the config directory. Without an argument it reads from `/app/config`.

The config directory must contain `connector.yaml` and a `mappings/` directory. ICC mappings connect local Core TimeSeries IDs to external Core TimeSeries IDs and use the same `direction` field as protocol connectors.

`connector.yaml`:

```yaml
core:
  baseUrl: "http://core.local:8080/"
keycloak:
  tokenUrl: "http://keycloak.local:8082/realms/pegelhub/protocol/openid-connect/token"
  clientId: "icc-core"
  clientSecret: "secret"
externalCore:
  core:
    baseUrl: "http://external-core.local:8080/"
  keycloak:
    tokenUrl: "http://external-keycloak.local:8082/realms/pegelhub/protocol/openid-connect/token"
    clientId: "icc-external"
    clientSecret: "secret"
schedule:
  delay: "1h"
mappingsDir: "mappings"
```

`mappings/water-level.yaml`:

```yaml
timeSeriesId: "11111111-1111-1111-1111-111111111111"
externalTimeSeriesId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
direction: "core-to-external"
```

## Docker

```sh
scripts/build-connector-image.sh icc-connector

docker run --rm -d \
  -v "$(pwd)/examples/config:/app/config:ro" \
  pegelhub-icc-connector:local
```
