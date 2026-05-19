# Inter-Cluster-Communication Connector

This connector synchronizes selected supplier data between two Pegelhub clusters.

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker
- Network access to both Pegelhub clusters

## Build

Build from the repository root:

```sh
mvn -pl connectors/icc-connector -am -DskipTests package
```

The build produces:

- `target/icc-connector-1.0.0-SNAPSHOT.jar`
- `target/lib/*.jar`

You do not need to build `library` separately anymore; the reactor handles it.

## Configuration

The connector accepts an optional first CLI argument pointing to the config directory.
Without an argument it reads from `/app/config`.

The config directory must contain:

- `connector.properties`
- `source-pegelhub.yaml`
- `sink-pegelhub.yaml`

Important `connector.properties` keys:

- `Core.Source`
- `Core.Sink`
- `Icc.RefreshInterval`
- `Icc.SourceStationNumber`

Important notes:

- `source-pegelhub.yaml` is used for the source Pegelhub cluster.
- `sink-pegelhub.yaml` is used for the target Pegelhub cluster.
- Each side needs its own pre-provisioned Keycloak client.
- `Icc.RefreshInterval` supports `24h`, `20m`, `45s` style values.

Checked-in examples live under `examples/config/`.

## Docker

Build the image from the connector directory:

```sh
cd connectors/icc-connector
docker build -t icc-connector .
```

Run the container with a directory mounted to `/app/config`:

```sh
docker run --rm -d \
  -v "$(pwd)/examples/config:/app/config:ro" \
  icc-connector
```

Use any host directory you want, as long as it contains the three configuration files.

## Notes

- The two Pegelhub clusters only need separate reachable HTTP(S) addresses; they do not need to run on separate machines.
- `supplier.connector.number` / `supplier.id` and `taker.connector.number` / `taker.id` must be unique inside their respective Pegelhub clusters.
