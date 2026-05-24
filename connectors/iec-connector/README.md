# IEC Connector

The IEC connector exchanges telemetry and measurement data via IEC 60870-5-104.

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker
- A reachable Pegelhub Core
- A reachable IEC 60870-5-104 endpoint

## Build

Build from the repository root:

```sh
mvn -pl connectors/iec-connector -am -DskipTests package
```

The build produces:

- `target/iec-connector.jar`
- `target/lib/*.jar`

## Configuration

Prepare a host directory, for example `iec_directory`, with:

- `/app/config/connector.properties`
- `/app/config/pegelhub.yaml`
- `/app/data/datapoints/*.yaml`

The connector accepts an optional first CLI argument pointing to the config directory.
Without an argument it reads from `/app/config`.

Important `connector.properties` keys include:

- `Connector.IsReadingFromIec`
- `DataPointsDir`
- `DelayInterval`
- `Iec.CommonAddress`
- `Iec.StartDtRetries`
- `StationNumbers`

`pegelhub.yaml` contains the Pegelhub supplier/taker registration data and Keycloak client credentials.
Set `DataPointsDir=/app/data/datapoints` for the container layout.

Checked-in examples live under:

- `examples/config/`
- `examples/data/datapoints/`

## Docker

Build the image from the connector directory:

```sh
cd connectors/iec-connector
docker build -t iec-connector .
```

Run the container:

```sh
docker run --name iec-connector -d \
  -v "$(pwd)/examples/config:/app/config:ro" \
  -v "$(pwd)/examples/data:/app/data:ro" \
  iec-connector
```

## Testing

`at/pegelhub/connector/iec/sample/SampleServer.java` is the local IEC sample server used during development and testing.

There is also an external test server project from FreyrSCADA:
https://github.com/FreyrSCADA/IEC-60870-5-104

## References

- [openmuc j60870](https://www.openmuc.org/j60870-release-1-5-0/)
- [Beckhoff TF6500 documentation](https://infosys.beckhoff.com/english.php?content=../content/1033/tf6500_tc3_iec60870_5_10x/984065803.html&id=9038877514577555054)
