# TSTP Connector

The TSTP connector exchanges measurements between PegelHub Core and a TSTP
server. Each mapping connects one Core time series to one TSTP station and
parameter. A process can import and export different mappings through the same
TSTP endpoint.

Outbound writes replace data within the submitted raw-layer span. Before
enabling exports, complete the [raw-data ownership and rollout checks](#raw-data-ownership-and-rollout-gate).

## Build and Test

Use JDK 21 and Maven 3.9 for tests, and Docker for the image build. Run from the
repository root:

```bash
mvn -B -ntp -pl connectors/tstp-connector -am test
scripts/build-connector-image.sh tstp-connector
```

The image is tagged `pegelhub-tstp-connector:local`. The image build compiles the
application itself but skips tests, so run both commands when validating a change.

## Configure

The first command-line argument selects the configuration directory; containers
default to `/app/config`. The directory must contain `connector.yaml` and at
least one mapping YAML file. See [`examples/config/`](examples/config/) for the
complete shape. Configuration is loaded once at startup; restart after editing
it. YAML does not expand environment variables.

Create a private working copy outside the repository:

```bash
CONFIG_ROOT="${XDG_CONFIG_HOME:-$HOME/.config}/pegelhub"
install -d -m 700 "$CONFIG_ROOT"
test -d "$CONFIG_ROOT/tstp-connector" || \
  cp -R connectors/tstp-connector/examples/config "$CONFIG_ROOT/tstp-connector"
chmod 700 "$CONFIG_ROOT/tstp-connector"
chmod 600 "$CONFIG_ROOT/tstp-connector/connector.yaml"
```

| Configuration key | Meaning |
| --- | --- |
| `core.baseUrl` | Core application root URL, with a trailing `/` |
| `core.authentication` | `tokenUrl`, `clientId`, and `clientSecret` for client-credentials access |
| `polling.interval` | Required positive duration, such as `15m`; accepts `s`, `m`, or `h` |
| `polling.overlap` | Positive replay duration in the same format; defaults to `1h` |
| `mappings.directory` | Mapping directory relative to the config root; defaults to `mappings` |
| `tstp.server.host`, `tstp.server.port` | Required endpoint host and TCP port |
| `tstp.server.timeOffset` | Fixed wire-clock UTC offset; defaults to `Z` |
| `tstp.server.writeFormat` | Outbound `binary` (default) or `ascii` |

The connector constructs `http://<host>:<port>/` and does not configure HTTPS
or TSTP authentication. Keycloak credentials authenticate Core requests only,
not the TSTP endpoint. Use a protected network path and verify that the server
accepts the required requests from the connector host. TSTP requests use a
10-second connect timeout and a 30-second request timeout.

### Server Time

`tstp.server.timeOffset` is the fixed offset of the server's wire clock from
UTC, default `Z` (also used when omitted). For a server whose `12:00:00` means
`11:00:00Z`, set `timeOffset: "+01:00"`. The same offset is used for query
windows, decoded timestamps, and outbound timestamps; Core always uses UTC
instants. TSTP query strings retain their literal `Z` suffix even when the
server uses an offset clock. This setting does not change polling overlap or
the container timezone. Regional zones such as `Europe/Vienna` are rejected:
do not infer daylight-saving rules from a single observed offset. Confirm the
server's year-round time convention before activation, especially for writes.

### Mappings and Access

Example mapping (`stationId` is the TSTP station number, not a Core Station UUID):

```yaml
timeSeriesId: "11111111-1111-1111-1111-111111111111"
stationId: 123
direction: "external-to-core"
```

- `external-to-core` reads the TSTP station and writes the Core time series.
- `core-to-external` reads the Core time series and writes the TSTP station.

Mapping files are loaded in sorted filename order. Startup rejects duplicate
routes, duplicate outbound `(stationId, parameter)` targets, duplicate inbound
Core targets, and directed feedback cycles within this process's configuration.
Different parameters at the same station are independent targets. Review routes
across other connector processes separately; there is no deployment-wide cycle
check.

Configure the Keycloak client for the `pegelhub-core-api` audience and only the
direction-appropriate lowercase Core roles: `measurement:write` for imports
and `measurement:read` for exports. Core metadata must already exist. The client
also needs the registration, source assignment, and read-access relations
described in the [library authorization prerequisites](../library/#core-authorization-prerequisites).

### Parameters and Units

Each mapping selects one `parameter` and its wire `unit`. Omitted parameters
default to `Wasserstand`; an omitted unit uses the parameter's default below.
The minimal mapping above therefore selects `Wasserstand` in `cm`.

| TSTP parameter | Supported wire units (default first) | Core property | Core output representation |
| --- | --- | --- | --- |
| `Wasserstand` | `cm` | `water-level` | `canonical` |
| `WTemperatur` | degrees Celsius (`"\u00b0C"` in YAML) | `water-temperature` | `canonical` |
| `Abfluss` | `m3/s`, `m^3/s`, `"m\u00b3/s"`, `l/s` | `discharge` | `litres-per-second` for `l/s`; otherwise `canonical` |

Use the exact parameter name and unit reported by the server, not a translated
label such as `Wassertemperatur`. Unsupported combinations fail at startup.
For example, these are two separate mapping files for one station:

```yaml
# temperature.yaml
timeSeriesId: "22222222-2222-2222-2222-222222222222"
stationId: 123
direction: "core-to-external"
parameter: "WTemperatur"
unit: "\u00b0C"
```

```yaml
# discharge.yaml
timeSeriesId: "33333333-3333-3333-3333-333333333333"
stationId: 123
direction: "core-to-external"
parameter: "Abfluss"
unit: "l/s"
```

Core owns all conversions. For outbound `l/s`, the connector requests
`litres-per-second` from Core and sends the returned values unchanged. It does
not multiply or divide locally. Temperature uses Core's canonical `Cel` values
with the TSTP degrees-Celsius label, without changing the number.

For inbound mappings, Core's source assignment must use the matching input
representation: `litres-per-second` for `l/s`, otherwise `canonical`. Select
the matching Core property in the table above. Neither the mapping nor the
TSTP catalog changes or validates that Core source assignment automatically;
review it before activation. See [measurement representations](../../docs/guides/measurement-representations.md).

### Catalog Validation

The connector queries the TSTP catalog using the mapping's station and
parameter with `Hauptreihe=true`. Exactly one confirmed main-series entry with
a ZRID and matching station, parameter and unit is required; it never chooses
the first of several matches. Valid entries are cached per station and parameter
for 24 hours. Unit checks also apply to cache hits and to each measurement
response's `DEF EINHEIT`. Invalid catalogs are not cached and are retried on
the next poll. Ambiguous or incomplete catalogs fail the mapping rather than
transferring unverified data.

TSTP XML responses are decoded using their XML encoding declaration. Outbound
XML declares and uses ISO-8859-1, including the degrees-Celsius unit. Binary
payloads use 32-bit floating-point values; text writes use a decimal string of
the Core value. Neither path applies blanket rounding to two decimal places.

## Synchronization Behavior

TSTP writes succeed only when the response message is exactly `confirm`
(case-insensitive, ignoring surrounding whitespace). A negative or ambiguous
confirmation leaves the mapping's earlier boundary intact for retry. One worker
processes mappings serially; a failed mapping does not skip later mappings.
The first cycle starts immediately, and later cycles start one
`polling.interval` after the previous cycle finishes.

Both directions reread recent data using optional `polling.overlap`, default
`1h`. It accepts the same positive `s`, `m`, or `h` literals as the interval.
The first read is `[cycle time - polling interval - overlap, cycle time)`.
After success, including an empty result, the next start is the cycle time
minus overlap, unless the previous start is later. Both sources are filtered to
half-open boundaries, and cycle times remain truncated to whole seconds. Fixed-delay
polling includes processing time in the next window without adding a delivery
delay. TSTP reads request recorded points (`WERTE=True`). Some servers still
interpolate the requested boundaries, so each HTTP read expands the wire window
by one second on both sides and filters decoded UTC readings back to the exact
logical `[from, until)` window. This removes synthetic query edges without
discarding a real reading at `from`; it does not drop the first/last record or
assume a sampling grid. Reads omit `QUAL`, using the server's default
highest-available-quality selection. This preserves stored series points, not
necessarily original sensor samples: any server-side processing already present
inside the series is unchanged.

The exact single-precision TSTP gap marker (`4E+37`, bits `0x7df0bdc2`) is
skipped before conversion to a measurement. Valid neighboring readings and
real zeroes are preserved. No replacement values are generated and no existing
Core data is deleted. A gap-only result is a successful empty poll, not a Core
write or evidence of fresh measurements. Other malformed/non-finite values
still fail the mapping instead of being silently treated as gaps.

The catalog cache and synchronization boundaries exist only in process memory.
After restart, each mapping reads one polling interval plus overlap, which can
replay values already transferred. Failed mappings keep their previous start
boundary and retry an enlarged window on the next cycle. After successful
polls, arrivals older than the overlap can still
be missed. Size overlap for the cumulative observation-to-source-visibility
delay, including IEC and ICC upstream. One hour allows margin for normal
5-15-minute upstream polling; slower upstream polling needs a larger overlap
plus delay margin (for example, `2h` with hourly ICC). This is a sizing example,
not a delivery guarantee. Replay is limited to recent data, not historical
backfill or a durable checkpoint.

### Wire Formats

Inbound GET responses remain Base64-encoded binary. Outbound PUT requests default
to binary for backward compatibility. Set `tstp.server.writeFormat` explicitly
to `ascii` to use text writes, or `binary` to retain binary writes. Other values
are rejected during configuration loading. For example:

```yaml
tstp:
  server:
    host: tstp.example.invalid
    port: 8032
    timeOffset: "+01:00"
    writeFormat: ascii
```

Text writes use time/value pairs inside the same TSTP XML envelope: `LEN="0"`,
`ANZ` equal to the number of measurements, and one `YYYY-MM-DDThh:mm:ssZ value`
pair per line. Values use a decimal point independent of the process locale. The unit
and configured server time offset are unchanged; milliseconds are omitted,
not rounded into the next second. This is a wire-format change, not a change
to sampling, measurement units, quality layers or polling windows.

The [TSTP specification](https://www.toposoft.de/formate_protokolle/tstp_protokoll.pdf)
describes the text data format in section 6.2 and ASCII GET in section 4.2;
the PUT example in section 4.3 uses binary. Text PUT acceptance is therefore
an explicit compatibility gate to verify on each target server, not a claim
of universal server support. Text payloads are larger than binary payloads.
There is no automatic format fallback: it could silently reintroduce timestamp
changes. An unconfirmed PUT fails the mapping and retains its retry window.
Empty batches, non-finite values and the reserved gap marker are rejected
rather than written as measurements in either format.

### Raw-Data Ownership and Rollout Gate

Outbound writes explicitly select raw quality layer `0` (`QUAL=0`).
PegelHub must be the sole writer of that mapped raw layer; corrections belong
in higher TSTP quality layers. Each write contains the complete sorted source
interval, including previously sent neighbors. Do not send only the newly
discovered late readings: TSTP replaces data within the submitted span.
The connector does not read/merge destination edits or write correction layers.
See the [TSTP specification, sections 4.2-4.4](https://www.toposoft.de/formate_protokolle/tstp_protokoll.pdf).

Before enabling this in production, use an approved test series on the deployed
TSTP version/configuration:

1. Confirm the configured PUT format is accepted; text PUT support must be
   verified explicitly when using `ascii`. Read back timestamps with seconds
   not on a five-second grid, values, and units. Include the configured time
   offset and a date boundary. Do not assume binary and text writes behave
   identically on a particular server version.
2. Write raw readings with an intermediate reading initially absent, and put a
   correction in a higher layer.
3. Deliver the missing raw reading through a complete overlapping interval,
   then replay that interval again.
4. Read back raw and corrected layers. Verify the late reading, its neighbors,
   surrounding data, and the higher-layer correction. Also verify recorded-point
   reads do not synthesize boundary values.
5. Check configured post-PUT actions tolerate repeated writes.

Unit tests verify connector behavior, not the actual server's layer isolation
or post-write actions. Treat an unperformed server check as an open rollout
gate. There is no exactly-once guarantee.

## Run the Image

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
