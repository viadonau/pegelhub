# FTP Connector

The FTP connector polls one FTP directory, parses `.asc` or `.zrxp` files, and
writes the resulting measurements to one Core time series. It supports import
into Core only.

## Build and Test

Use JDK 21 and Maven 3.9 for tests, and Docker for the image build. Run from the
repository root:

```bash
mvn -B -ntp -pl connectors/ftp-connector -am test
scripts/build-connector-image.sh ftp-connector
```

The image is tagged `pegelhub-ftp-connector:local`. The image build compiles the
application itself but skips tests, so run both commands when validating a change.

## Configure

The process reads `connector.yaml` and mapping YAML files from a configuration
directory. The first command-line argument selects that directory; containers
default to `/app/config`. Start from the checked-in
[`examples/config/`](examples/config/) schema, but replace every endpoint and
credential placeholder for the target environment. Configuration is loaded once
at startup; restart after editing it. YAML does not expand environment variables.

Create a private working copy outside the repository:

```bash
CONFIG_ROOT="${XDG_CONFIG_HOME:-$HOME/.config}/pegelhub"
install -d -m 700 "$CONFIG_ROOT"
test -d "$CONFIG_ROOT/ftp-connector" || \
  cp -R connectors/ftp-connector/examples/config "$CONFIG_ROOT/ftp-connector"
chmod 700 "$CONFIG_ROOT/ftp-connector"
chmod 600 "$CONFIG_ROOT/ftp-connector/connector.yaml"
```

| Configuration key | Meaning |
| --- | --- |
| `core.baseUrl` | Core application root URL, with a trailing `/` |
| `core.authentication` | `tokenUrl`, `clientId`, and `clientSecret` for client-credentials access |
| `polling.interval` | Required positive duration, such as `15m`; accepts `s`, `m`, or `h` |
| `mappings.directory` | Mapping directory relative to the config root; defaults to `mappings` |
| `ftp.server` | Required `host`, `port`, and `authentication.username` / `authentication.password` |
| `ftp.source` | Required remote `directory` and `parserType` (`asc` or `zrxp`) |

The transport is plain FTP in passive mode, not SFTP or FTPS. Use a protected
network path; credentials and file content are not encrypted by this connector.

### Mapping and Units

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
and case-insensitive. A missing or unsupported parameter fails startup.

| Parameter | Accepted source units | Value submitted to Core | Core source representation |
| --- | --- | --- | --- |
| `Wasserstand` | `cm`, `mm` | Centimetres; millimetres are divided by 10 | `canonical` |
| `WasserstandAbs` | `mua`, m&uuml;a | Metres above Adria, unchanged | `metres-above-adria` |
| `Abfluss` | `m3/s`, m&sup3;/s, `l/s`, `cumc` | Cubic metres per second; litres per second are divided by 1,000 | `canonical` |
| `WTemperatur` | &deg;C, `C`, `Cel` | Degrees Celsius, unchanged | `canonical` |

Unit matching ignores case, spaces, and periods. Entries with unsupported units
are logged and skipped. Unlike IEC and TSTP, FTP normalizes `mm` and `l/s`
before submitting them: do not configure a second conversion in Core. Absolute
water levels require the gauge zero on Core's measuring point. See
[measurement representations](../../docs/guides/measurement-representations.md).

ZRXP timestamps are interpreted as UTC. ASC timestamps have no source offset
and are interpreted in the connector JVM's default timezone. Set and verify the
runtime timezone when importing ASC files.

Both parsers read ISO-8859-1 text and support the source layout implemented in
this module, not every variant of these formats. ZRXP obtains station and
parameter from the second and third underscore-separated parts of `REXCHANGE`.
Its `RINVAL` header is ignored; numeric missing-value markers are not filtered
out automatically. Check representative source files and remove or transform
such markers before ingestion. ASC examples are available under
[`examples/data/`](examples/data/).

The Keycloak client needs a token for the `pegelhub-core-api` audience and the
Core role `measurement:write`. Register the same client ID as Connector
metadata in Core before ingesting measurements. This connector does not submit
telemetry and does not need `telemetry:write`. Core also requires the target
time series source assignment to use the matching connector and the complete
metadata hierarchy to be active. See the
[library authorization prerequisites](../library/#core-authorization-prerequisites).

## Import Behavior

The first poll starts immediately. Later polls start one `polling.interval`
after the previous job finishes. Each poll considers regular files with the
case-sensitive suffix selected by `parserType` (`.asc` or `.zrxp`) whose FTP
modification time is strictly newer than the current time minus one polling
interval. There is no recursive scan or historical-file import. Files can age
out of this window while the connector is stopped or a cycle is slow.

Processed filenames are kept only in process memory. A file whose content or
modification time changes under an already processed filename is not
reconsidered until the connector restarts.

A filename is marked processed after parsing and before the batch is submitted
to Core. A Core submission failure is therefore not retried for that file by
the same process. Restarting clears the processed-name set and can replay a
still-recent file. This connector is not a durable or exactly-once import queue;
monitor failed submissions and reconcile them operationally.

## Run the Image

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
