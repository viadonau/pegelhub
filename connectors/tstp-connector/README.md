# Time-Series-Transfer-Protocol Connector

This connector reads from or writes to a TSTP server and exchanges the data with Pegelhub Core.

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker
- Access to a Pegelhub Core and a TSTP server

## Build

Build from the repository root:

```sh
mvn -pl connectors/tstp-connector -am -DskipTests package
```

The build produces:

- `target/tstp-connector.jar`
- `target/lib/*.jar`

## Configuration

The connector accepts an optional first CLI argument pointing to the config directory.
Without an argument it reads from `/app/config`.

The config directory must contain:

- `connector.properties`
- `pegelhub.yaml`

Important `connector.properties` keys:

- `core.address`
- `core.port`
- `tstp.address`
- `tstp.port`
- `connector.readDelay`

`pegelhub.yaml` contains the Pegelhub registration data and Keycloak client credentials.

Behavior depends on `isSupplier` in `pegelhub.yaml`:

- `true`: read from TSTP and send to Pegelhub
- `false`: read from Pegelhub and write to TSTP

Checked-in examples live under `examples/config/`.

## Docker

Build the image from the connector directory:

```sh
cd connectors/tstp-connector
docker build -t tstp-connector:latest .
```

Run the container:

```sh
docker run --rm -d \
  -v "$(pwd)/examples/config:/app/config:ro" \
  tstp-connector:latest
```

Make sure the mounted config directory contains both configuration files.

## Notes

- `connector.readDelay` uses the format `number[s/m/h]`.
- For local debugging outside Docker, you can run the thin jar with the packaged dependency directory:

```sh
java -cp "target/tstp-connector.jar:target/lib/*" \
  at.pegelhub.connector.tstp.Main /path/to/config-dir
```
