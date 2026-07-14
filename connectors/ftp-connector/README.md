# FTP Connector

This connector fetches files from an FTP server, parses `.asc` or `.zrxp` measurements, and forwards them to Pegelhub Core.

## Build

```sh
mvn -pl connectors/ftp-connector -am -DskipTests package
```

## Configuration

The connector accepts an optional first CLI argument pointing to the config directory. Without an argument it reads from `/app/config`.

The config directory must contain `connector.yaml` and a `mappings/` directory. The FTP connector currently
requires exactly one `external-to-core` mapping file.

`connector.yaml`:

```yaml
core:
  baseUrl: "http://localhost:8080/"
keycloak:
  tokenUrl: "http://localhost:8082/realms/pegelhub/protocol/openid-connect/token"
  clientId: "connector"
  clientSecret: "secret"
schedule:
  delay: "15m"
mappingsDir: "mappings"
ftp:
  address: "ftp.viadonau.org"
  port: 21
  user: "pegelReader"
  password: "securePassword123"
  path: "/"
  parserType: "zrxp"
```

`mappings/station.yaml`:

```yaml
timeSeriesId: "11111111-1111-1111-1111-111111111111"
stationId: 1
parameter: "Wasserstand"
direction: "external-to-core"
```

`parameter` is optional and used by the ZRXP parser. FTP only supports `external-to-core` in Phase 2.

## Docker

```sh
scripts/build-connector-image.sh ftp-connector

docker run --rm -d \
  -v "$(pwd)/examples/config:/app/config:ro" \
  pegelhub-ftp-connector:local
```
