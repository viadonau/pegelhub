# FTP connector

The FTP connector polls one FTP directory, parses `.asc` or `.zrxp` files, and
writes the resulting measurements to one Core time series. It supports import
into Core only.

## Build

From the repository root:

```bash
mvn -B -ntp -pl connectors/ftp-connector -am test
scripts/build-connector-image.sh ftp-connector
```

The image is tagged `pegelhub-ftp-connector:local`.

## Configure

The process reads `connector.yaml` and mapping YAML files from a configuration
directory. The first command-line argument selects that directory; containers
default to `/app/config`. Start from the checked-in
[`examples/config/`](examples/config/) schema, but replace every endpoint and
credential placeholder for the target environment.

Create a private working copy outside the repository:

```bash
CONFIG_ROOT="${XDG_CONFIG_HOME:-$HOME/.config}/pegelhub"
install -d -m 700 "$CONFIG_ROOT"
test -d "$CONFIG_ROOT/ftp-connector" || \
  cp -R connectors/ftp-connector/examples/config "$CONFIG_ROOT/ftp-connector"
chmod 700 "$CONFIG_ROOT/ftp-connector"
chmod 600 "$CONFIG_ROOT/ftp-connector/connector.yaml"
```

`connector.yaml` configures:

- `core.baseUrl` and `core.authentication` for client-credentials access
- a positive `polling.interval` ending in `s`, `m`, or `h`
- `mappings.directory`, which defaults to `mappings` when omitted
- FTP host, port, username, password, source directory, and `parserType`

`parserType` is `asc` or `zrxp`. Exactly one mapping file is required:

```yaml
timeSeriesId: "11111111-1111-1111-1111-111111111111"
stationId: 1
parameter: "Wasserstand"
direction: "external-to-core"
```

`direction` may be omitted because FTP defaults it to `external-to-core`; no
other direction is accepted. `stationId` is the integer `location` parsed from
the source file, not a Core Station UUID. A non-negative value filters to that
location; a negative value disables location filtering. `parameter` is required
and case-insensitive. Use `Wasserstand` for canonical water-level values,
`WasserstandAbs` for metres-above-Adria water levels, `Abfluss` for discharge,
or `WTemperatur` for water temperature. A missing or other parameter is
rejected during startup, so one mapping cannot mix physical source
representations. Mapping files are
loaded in sorted filename order, although FTP requires exactly one.

ZRXP timestamps are interpreted as UTC. ASC timestamps have no source offset
and are interpreted in the connector JVM's default timezone. Set and verify the
runtime timezone when importing ASC files.

The Keycloak client needs a token for the `pegelhub-core-api` audience and the
Core role `measurement:write`. Register the same client ID as Connector
metadata in Core before ingesting measurements. This connector does not submit
telemetry and does not need `telemetry:write`. Core also requires the target
time series source assignment to use the matching connector and the complete
metadata hierarchy to be active. See the
[library authorization prerequisites](../library/#core-authorization-prerequisites).

## Import behavior

Each poll considers regular files with a case-sensitive `.asc` or `.zrxp`
suffix whose FTP modification time is strictly newer than one polling interval
before that poll. Processed filenames are kept only in process memory. A file
whose content or modification time changes under an already processed filename
is not reconsidered until the connector restarts.

A filename is marked processed after parsing and before the batch is submitted
to Core. A Core submission failure is therefore not retried for that file by
the same process. Restarting clears the processed-name set and can replay a
still-recent file. This connector is not a durable or exactly-once import queue;
monitor failed submissions and reconcile them operationally.

## Run the image

```bash
CONFIG_DIR="${XDG_CONFIG_HOME:-$HOME/.config}/pegelhub/ftp-connector"
docker run --rm \
  -v "${CONFIG_DIR}:/app/config:ro" \
  pegelhub-ftp-connector:local
```

Edit the copied schema before running. Its `.invalid` endpoints and placeholder
credentials are intentionally unusable. Core, Keycloak, and FTP addresses must
be resolvable and reachable from inside the connector container. Never commit
real client or FTP secrets.

For Compose-based deployments, use the
[shared connector runner](../../deploy/connector/).
