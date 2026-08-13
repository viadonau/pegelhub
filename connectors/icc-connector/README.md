# ICC connector

The ICC connector periodically copies recent time-series measurements between
a local PegelHub Core instance and a remote PegelHub Core instance. Each mapping
chooses its direction independently.

## Build

From the repository root:

```bash
mvn -B -ntp -pl connectors/icc-connector -am test
scripts/build-connector-image.sh icc-connector
```

The image is tagged `pegelhub-icc-connector:local`.

## Configure

The first command-line argument selects the configuration directory; containers
default to `/app/config`. The directory must contain `connector.yaml` and at
least one mapping YAML file. Start from [`examples/config/`](examples/config/)
and replace its illustrative hosts and credentials.

Create a private working copy outside the repository:

```bash
CONFIG_ROOT="${XDG_CONFIG_HOME:-$HOME/.config}/pegelhub"
install -d -m 700 "$CONFIG_ROOT"
test -d "$CONFIG_ROOT/icc-connector" || \
  cp -R connectors/icc-connector/examples/config "$CONFIG_ROOT/icc-connector"
chmod 700 "$CONFIG_ROOT/icc-connector"
chmod 600 "$CONFIG_ROOT/icc-connector/connector.yaml"
```

`connector.yaml` defines `localCore` and `remoteCore`, each with `baseUrl` and
client-credentials authentication. It also defines a positive polling interval
ending in `s`, `m`, or `h`. `mappings.directory` defaults to `mappings`.

Each mapping relates one local and one remote Core time series:

```yaml
timeSeriesId: "11111111-1111-1111-1111-111111111111"
externalTimeSeriesId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
direction: "core-to-external"
```

- `core-to-external` reads `timeSeriesId` from local Core and writes
  `externalTimeSeriesId` to remote Core.
- `external-to-core` reads `externalTimeSeriesId` from remote Core and writes
  `timeSeriesId` to local Core.

Mapping files are processed in sorted filename order. Configure each Keycloak
client for the `pegelhub-core-api` audience and only the read or write roles
needed on that side, using the lowercase runtime values such as
`measurement:read` and `measurement:write`.
Each Core client also needs the registration and resource grants described in
the [library authorization prerequisites](../library/#core-authorization-prerequisites).

## Transfer behavior

Every cycle reads the source's relative lookback window, whose duration equals
the configured polling interval, rewrites each measurement to the target time
series ID, and submits that batch. Mapping failures are logged independently so
later mappings still run.

The current implementation also submits when the source returns no points.
Current Core rejects an empty measurement batch, so an empty source window is
logged as a mapping failure rather than a no-op.

The connector stores no cursor, copied-point ledger, retry queue, or durable
state. Because the scheduler uses a fixed delay after processing, processing
time can leave a gap between successive lookback windows. Late source data
outside the current window can be missed. A restart less than one polling
interval after the prior cycle can overlap that cycle's lookback and resubmit
data. Treat this as best-effort recent-window copying, not a lossless or
exactly-once replication mechanism.

## Run the image

```bash
CONFIG_DIR="${XDG_CONFIG_HOME:-$HOME/.config}/pegelhub/icc-connector"
docker run --rm \
  -v "${CONFIG_DIR}:/app/config:ro" \
  pegelhub-icc-connector:local
```

The checked-in configuration is a schema example, not a working environment.
Both Core and Keycloak addresses must be reachable from inside the connector
container. Keep real client secrets in an ignored, read-only mounted directory.
