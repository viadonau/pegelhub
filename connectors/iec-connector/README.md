# IEC Connector

The IEC connector exchanges telemetry and measurement data via IEC 60870-5-104.

## Build

```sh
mvn -pl connectors/iec-connector -am -DskipTests package
```

## Configuration

The connector accepts an optional first CLI argument pointing to the config directory. Without an argument it reads from `/app/config`.

The config directory must contain `connector.yaml` and a `mappings/` directory.

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
iec:
  address: "127.0.0.1"
  port: 2404
  commonAddress: 1
```

Mapping files are read in sorted filename order:

```yaml
iecIoa: 66049
timeSeriesId: "11111111-1111-1111-1111-111111111111"
direction: "external-to-core"
```

Use `direction: "external-to-core"` for IEC values sent to Core and `direction: "core-to-external"` for Core measurements written to IEC.

## Docker

```sh
scripts/build-connector-image.sh iec-connector

docker run --rm -d \
  -v "$(pwd)/examples/config:/app/config:ro" \
  pegelhub-iec-connector:local
```

## References

- [openmuc j60870](https://www.openmuc.org/j60870-release-1-5-0/)
- [Beckhoff TF6500 documentation](https://infosys.beckhoff.com/english.php?content=../content/1033/tf6500_tc3_iec60870_5_10x/984065803.html&id=9038877514577555054)
