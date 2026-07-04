# Inter-Cluster-Communication Connector

This connector synchronizes selected time-series measurements between two Pegelhub clusters.

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

- `target/icc-connector.jar`
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
- `Icc.SourceTimeSeriesId`

Important notes:

- `source-pegelhub.yaml` is used for the source Pegelhub cluster.
- `sink-pegelhub.yaml` is used for the target Pegelhub cluster.
- Each side needs its own pre-provisioned Keycloak client.
- `Icc.RefreshInterval` supports `24h`, `20m`, `45s` style values.
- `Icc.SourceTimeSeriesId` accepts one or more UUIDs separated by commas.

Checked-in examples live under `examples/config/`.

## Docker

Build the image from the repository root:

```sh
scripts/build-connector-image.sh icc-connector
```

Run the container with a directory mounted to `/app/config`:

```sh
docker run --rm -d \
  -v "$(pwd)/examples/config:/app/config:ro" \
  pegelhub-icc-connector:local
```

Use any host directory you want, as long as it contains the three configuration files.

## Notes

- The two Pegelhub clusters only need separate reachable HTTP(S) addresses; they do not need to run on separate machines.
- `supplier.connector.number` / `supplier.id` and `taker.connector.number` / `taker.id` must be unique inside their respective Pegelhub clusters.
