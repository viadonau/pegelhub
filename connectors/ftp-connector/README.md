# File-Transfer-Protocol Connector

This connector fetches data from an FTP server and forwards parsed measurements to Pegelhub.
Supported input formats are `.asc` and `.zrxp`.

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker
- Network access to the FTP server and Pegelhub Core

## Build

Build from the repository root:

```sh
mvn -pl connectors/ftp-connector -am -DskipTests package
```

The build produces:

- `target/ftp-connector-1.0.0-SNAPSHOT.jar`
- `target/lib/*.jar`

## Configuration

The connector accepts an optional first CLI argument pointing to the config directory.
Without an argument it reads from `/app/config`.

The config directory must contain:

- `connector.properties`
- `pegelhub.yaml`

`pegelhub.yaml` contains the Pegelhub registration data and API token.
`connector.properties` contains the connector runtime settings.

Important `connector.properties` keys:

- `core.address`
- `core.port`
- `ftp.address`
- `ftp.port`
- `ftp.user`
- `ftp.password`
- `ftp.path`
- `parser.type`
- `read.delay`

Example:

```properties
core.address=127.0.0.1
core.port=8081
ftp.address=ftp.viadonau.org
ftp.port=21
ftp.user=pegelReader
ftp.password=securePassword123
ftp.path=/
parser.type=zrxp
read.delay=15m
```

Checked-in examples live under:

- `examples/config/`
- `examples/data/`

## Docker

Build the image from the connector directory:

```sh
cd connectors/ftp-connector
docker build -t ftp-connector .
```

Run the container with a directory mounted to `/app/config`:

```sh
docker run --rm -d \
  -v "$(pwd)/examples/config:/app/config:ro" \
  ftp-connector
```

Use any host directory you want, as long as it contains `connector.properties` and `pegelhub.yaml`.

## Notes

- Each connector instance needs its own API token.
- If you need both `.asc` and `.zrxp`, run separate connector instances.
- `read.delay` uses `number[s/m/h]`, for example `30s`, `15m`, `1h`.
