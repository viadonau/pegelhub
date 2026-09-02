# FTP connector

The FTP connector polls one FTP directory and writes parsed measurements to one
Core time series. Each process is configured for either `.asc` or `.zrxp`
files; it does not ingest both formats at once. It supports import into Core
only, over plain FTP (not FTPS or SFTP).

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
- a positive `polling.interval` ending in `s`, `m`, or `h` (case-insensitive)
- `mappings.directory`, which defaults to `mappings` when omitted
- FTP host, port, username, password, source directory, and `parserType`

`parserType` is `asc` or `zrxp` and selects both the parser and filename suffix.
Exactly one mapping file is required:

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
and determines which source units are accepted and how values are normalized
for Core:

| Parameter | Accepted source units | Value sent to Core | Required source representation |
| --- | --- | --- | --- |
| `Wasserstand` | `cm`, `mm` | centimetres (`mm` is divided by 10) | `canonical` |
| `WasserstandAbs` | `mua`, `müA` (spacing and dots ignored) | metres above Adria, unchanged | `metres-above-adria` |
| `Abfluss` | `m3/s`, `m³/s`, `l/s` | cubic metres per second (`l/s` is divided by 1,000) | `canonical` |
| `WTemperatur` | `°C`, `C`, `CEL` | degrees Celsius, unchanged | `canonical` |

Parameter and unit matching is case-insensitive. Unsupported or missing units
are skipped. A missing or unsupported mapping parameter is rejected during
startup, so one mapping cannot mix physical source representations. Mapping
files are loaded in sorted filename order, although FTP requires exactly one.

ZRXP timestamps are interpreted as UTC. ASC timestamps have no source offset
and are interpreted in the connector JVM's default timezone. Set and verify the
runtime timezone when importing ASC files. ZRXP observations equal to the
block's declared `RINVAL` sentinel are discarded.

The Keycloak client needs a token for the `pegelhub-core-api` audience and the
Core role `measurement:write`. Register the same client ID as Connector
metadata in Core before ingesting measurements. This connector does not submit
telemetry and does not need `telemetry:write`. Core also requires the target
time series source assignment to use the matching connector and the
representation listed above, and the complete metadata hierarchy to be active.
See the
[library authorization prerequisites](../library/#core-authorization-prerequisites).

## Import behavior

At startup, the connector fixes its modification-time cutoff at one polling
interval before startup. Each poll considers regular files with the configured,
case-sensitive `.asc` or `.zrxp` suffix that are newer than that cutoff.
Processed `(filename, modification time)` pairs are kept only in process
memory. Replacing a file under the same name is reconsidered when the FTP
modification time changes; a content-only change with the same timestamp is
not.

Files are marked processed only after their FTP transfers complete and their
combined non-empty Core batch is accepted. A failed Core submission leaves the
files eligible for the next poll. Successfully parsed files that yield no
measurements are also marked processed. Restarting clears the processed set and
can replay files newer than the newly calculated startup cutoff. This connector
is not a durable or exactly-once import queue; monitor failed submissions and
reconcile them operationally.

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
