# TSTP connector

The TSTP connector exchanges measurements between PegelHub Core and a TSTP
server. One process can run inbound and outbound mappings while sharing its
endpoint, Core client, catalog cache, scheduler, and shutdown lifecycle.

## Build

From the repository root:

```bash
mvn -B -ntp -pl connectors/tstp-connector -am test
scripts/build-connector-image.sh tstp-connector
```

The image is tagged `pegelhub-tstp-connector:local`.

## Configure

The first command-line argument selects the configuration directory; containers
default to `/app/config`. The directory must contain `connector.yaml` and at
least one mapping YAML file. See [`examples/config/`](examples/config/) for the
complete shape.

Create a private working copy outside the repository:

```bash
CONFIG_ROOT="${XDG_CONFIG_HOME:-$HOME/.config}/pegelhub"
install -d -m 700 "$CONFIG_ROOT"
test -d "$CONFIG_ROOT/tstp-connector" || \
  cp -R connectors/tstp-connector/examples/config "$CONFIG_ROOT/tstp-connector"
chmod 700 "$CONFIG_ROOT/tstp-connector"
chmod 600 "$CONFIG_ROOT/tstp-connector/connector.yaml"
```

`connector.yaml` defines the Core URL and client-credentials authentication, a
positive polling interval ending in `s`, `m`, or `h` (case-insensitive), and
the TSTP server host and port. The implementation constructs an unauthenticated
plain HTTP endpoint; it has no HTTPS scheme or TSTP credential setting.
`mappings.directory` defaults to `mappings`.

Example mapping:

```yaml
timeSeriesId: "11111111-1111-1111-1111-111111111111"
stationId: 123
direction: "external-to-core"
```

- `external-to-core` reads the TSTP station and writes the Core time series.
- `core-to-external` reads the Core time series and writes the TSTP station.

Mapping files are loaded in sorted filename order. Startup rejects exact
duplicates, duplicate outbound station targets, duplicate inbound Core targets,
and directed feedback cycles. A failed mapping does not prevent the remaining
mappings in that polling cycle from running.

For every station, the connector queries the TSTP catalog with
`Parameter=Wasserstand` and `Hauptreihe=true`, then uses the first returned
ZRID. Resolved ZRIDs are cached in memory for 24 hours.

TSTP binary timestamps are interpreted and emitted as UTC with whole-second
precision. Inbound 32-bit floating-point values are rounded to two decimal
places and forwarded without unit conversion. Outbound values are cast to
32-bit floats and sent to the water-level ZRID selected above; the request
declares series type `Z` and unit `cm`. Both directions therefore require a Core
time series with observed property `water-level`, whose canonical unit is `cm`.
For `external-to-core`, its source assignment must use representation
`canonical`; `metres-above-adria` would incorrectly convert the incoming
centimetre value as an elevation.

Configure the Keycloak client for the `pegelhub-core-api` audience and only the
direction-appropriate lowercase Core roles, such as `measurement:read` and
`measurement:write`.
The client also needs the registration and read-access relations described in the
[library authorization prerequisites](../library/#core-authorization-prerequisites).

## Synchronization behavior

The first cycle reads the half-open window `[cycle time - polling interval,
cycle time)`. Successful mapping runs advance an in-memory boundary, and the
next window is `[previous cycle time, current cycle time)`. Both TSTP and Core
results are filtered to those exact boundaries. TSTP cycle times are truncated
to whole seconds to match the protocol. Polling uses fixed delay, so each new
window also covers the prior cycle's processing time.

The catalog cache and synchronization boundaries exist only in process memory.
After restart, each mapping starts again with a window equal to one polling
interval, which can replay values that were already transferred. Failed
mappings keep their previous start boundary and retry an enlarged window on the
next cycle. Late source data whose observation time falls in an already
completed window can be missed. There is no durable checkpoint or exactly-once
guarantee.

## Run the image

```bash
CONFIG_DIR="${XDG_CONFIG_HOME:-$HOME/.config}/pegelhub/tstp-connector"
docker run --rm \
  -v "${CONFIG_DIR}:/app/config:ro" \
  pegelhub-tstp-connector:local
```

Replace the checked-in illustrative endpoints and credentials in an ignored
copy before connecting to real systems. Core, Keycloak, and TSTP addresses must
be reachable from inside the connector container.

For Compose-based deployments, use the
[shared connector runner](../../deploy/connector/).
