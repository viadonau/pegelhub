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
positive polling interval ending in `s`, `m`, or `h`, and the TSTP server host
and port. The implementation constructs a plain HTTP endpoint; it does not
support an HTTPS scheme setting. `mappings.directory` defaults to `mappings`.

`tstp.server.timeOffset` is the fixed offset of the server's wire clock from
UTC, default `Z` (also used when omitted). For a server whose `12:00:00` means
`11:00:00Z`, set `timeOffset: "+01:00"`. The same offset is used for query
windows, decoded timestamps, and outbound timestamps; Core always uses UTC
instants. TSTP query strings retain their literal `Z` suffix even when the
server uses an offset clock. This setting does not change polling overlap or
the container timezone. Regional zones such as `Europe/Vienna` are rejected:
do not infer daylight-saving rules from a single observed offset. Confirm the
server's year-round time convention before activation, especially for writes.

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
ZRID. Valid catalog entries are cached in memory for 24 hours. Empty responses
or missing ZRIDs fail the mapping and are queried again on the next poll.

Configure the Keycloak client for the `pegelhub-core-api` audience and only the
direction-appropriate lowercase Core roles, such as `measurement:read` and
`measurement:write`.
The client also needs the registration and read-access relations described in the
[library authorization prerequisites](../library/#core-authorization-prerequisites).

## Synchronization behavior

Writes succeed only when the response message is exactly `confirm`
(case-insensitive, ignoring surrounding whitespace). A negative or ambiguous
confirmation leaves the mapping's earlier boundary intact for retry.

Both directions reread recent data using optional `polling.overlap`, default
`1h`. It accepts the same positive `s`, `m`, or `h` literals as the interval.
The first read is `[cycle time - polling interval - overlap, cycle time)`.
After success, including an empty result, the next start is the cycle time
minus overlap, never moving backwards. Both sources are filtered to half-open
boundaries, and cycle times remain truncated to whole seconds. Fixed-delay
polling includes processing time in the next window without adding a delivery
delay. TSTP reads request recorded points (`WERTE=True`). Some servers still
interpolate the requested boundaries, so each HTTP read expands the wire window
by one second on both sides and filters decoded UTC readings back to the exact
logical `[from, until)` window. This removes synthetic query edges without
discarding a real reading at `from`; it does not drop the first/last record or
assume a sampling grid. The existing highest-available-quality selection remains.
This preserves stored series points, not necessarily original sensor samples:
any server-side processing already present inside the series is unchanged.

The exact single-precision TSTP gap marker (`4E+37`, bits `0x7df0bdc2`) is
skipped before conversion to a measurement. Valid neighboring readings and
real zeroes are preserved. No replacement values are generated and no existing
Core data is deleted. A gap-only result is a successful empty poll, not a Core
write or evidence of fresh measurements. Other malformed/non-finite values
still fail the mapping instead of being silently treated as gaps.

The catalog cache and synchronization boundaries exist only in process memory.
After restart, each mapping reads one polling interval plus overlap, which can
replay values already transferred. Failed
mappings keep their previous start boundary and retry an enlarged window on the
next cycle. After successful polls, arrivals older than the overlap can still
be missed. Size overlap for the cumulative observation-to-source-visibility
delay, including IEC and ICC upstream. One hour allows margin for normal
5-15-minute polling along the planned route; use a larger overlap for slower
upstream polling (for example, `2h` with hourly ICC). This is bounded recent
replay, not historical backfill or a durable checkpoint.

### Raw-data ownership and rollout gate

Outbound writes explicitly select raw quality layer `0` (`QUAL=0`).
PegelHub must be the sole writer of that mapped raw layer; corrections belong
in higher TSTP quality layers. Each write contains the complete sorted source
interval, including previously sent neighbors. Do not send only the newly
discovered late readings: TSTP replaces data within the submitted span.
The connector does not read/merge destination edits or write correction layers.
See the [TSTP specification, sections 4.2-4.4](https://www.toposoft.de/formate_protokolle/tstp_protokoll.pdf).

Before enabling this in production, use an approved test series on the deployed
TSTP version/configuration:

1. Write raw readings with an intermediate reading initially absent, and put a
   correction in a higher layer.
2. Deliver the missing raw reading through a complete overlapping interval,
   then replay that interval again.
3. Read back raw and corrected layers. Verify the late reading, its neighbors,
   surrounding data, and the higher-layer correction. Also verify recorded-point
   reads do not synthesize boundary values.
4. Check configured post-PUT actions tolerate repeated writes.

Unit tests verify connector behavior, not the actual server's layer isolation
or post-write actions. Treat an unperformed server check as an open rollout
gate. There is no exactly-once guarantee.

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
