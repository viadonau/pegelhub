# IEC connector

The IEC connector exchanges measurements with an IEC 60870-5-104 server. A
mapping binds one IEC information object address (IOA) to one Core time series
and selects the transfer direction.

## Build

From the repository root:

```bash
mvn -B -ntp -pl connectors/iec-connector -am test
scripts/build-connector-image.sh iec-connector
```

The image is tagged `pegelhub-iec-connector:local`.

## Configure

The first command-line argument selects the configuration directory; containers
default to `/app/config`. The directory must contain `connector.yaml` and at
least one mapping YAML file. See [`examples/config/`](examples/config/) for the
complete shape.

Create a private working copy outside the repository:

```bash
CONFIG_ROOT="${XDG_CONFIG_HOME:-$HOME/.config}/pegelhub"
install -d -m 700 "$CONFIG_ROOT"
test -d "$CONFIG_ROOT/iec-connector" || \
  cp -R connectors/iec-connector/examples/config "$CONFIG_ROOT/iec-connector"
chmod 700 "$CONFIG_ROOT/iec-connector"
chmod 600 "$CONFIG_ROOT/iec-connector/connector.yaml"
```

`connector.yaml` defines the Core URL and client-credentials authentication, a
positive polling interval ending in `s`, `m`, or `h`, and the IEC server
`host`, `port`, and `commonAddress`. `mappings.directory` defaults to
`mappings`.

### Optional latest-value ingestion

By default, every received inbound measurement is forwarded at `polling.interval`.
To store only the last received value per IOA at each poll, set `ingestion.mode`
to `latest`. For a five-minute polling interval:

```yaml
polling:
  interval: 5m
ingestion:
  mode: latest
```

`ingestion` requires only `mode` (`all` or `latest`). Omitting the section
preserves the existing behavior: `all` at `polling.interval`. There is no separate
ingestion interval. Selection affects only IEC -> Core, but `polling.interval`
still controls both transfer directions: setting it to `5m` also makes Core -> IEC
send every five minutes. Connection recovery keeps its independent schedule.

Both modes keep the existing first poll one second after runtime startup. Later
polls run `polling.interval` after the previous job completes, not at
wall-clock-aligned five-minute boundaries. Each drain in `latest` mode selects
the last received reading per IOA since the preceding drain. It preserves that
reading's receipt timestamp and
value; this is a snapshot, not an average, and intermediate changes/peaks are
intentionally discarded. No new reading means no new snapshot. A newly received
unchanged value is still a reading and is forwarded with its receipt timestamp.

Selection occurs before adding snapshots to the pending retry batches. Failed
snapshots are retained alongside snapshots from later intervals, so a successful
retry can send more than one snapshot per IOA. This reduces Core writes and stored
points, not traffic from the IEC server: received values remain in memory until
the next drain. Existing history is not changed. Deploy an image supporting this
section before enabling it; older images reject unknown configuration fields.

Example mapping:

```yaml
iecIoa: 66049
timeSeriesId: "11111111-1111-1111-1111-111111111111"
direction: "external-to-core"
```

Use `external-to-core` for IEC values written to Core and `core-to-external`
for Core measurements written to IEC. Mapping files are loaded in sorted
filename order. Each IOA may appear only once across all mappings; duplicate
IOAs fail startup.

Outbound mappings may set `outputRepresentation: metres-above-adria` for water
level or `outputRepresentation: litres-per-second` for discharge. The default is
`canonical`. Core performs the conversion and must confirm the representation
and unit in its response; IEC forwards the result without further conversion.
Inbound units are declared on the Core source assignment, not on this mapping.

The former `gaugeZeroElevationMAboveAdria` mapping field is no longer accepted.
Set the gauge zero on the Core measuring point and replace that mapping field
with `outputRepresentation: metres-above-adria` before upgrading the connector.
Deploy the compatible Core first. See the
[representation upgrade guide](../../docs/guides/measurement-representations.md).

Core authentication must produce a token for the `pegelhub-core-api` audience
with the direction-appropriate lowercase role, such as `measurement:write` for
inbound values or `measurement:read` for outbound values.
The client also needs the registration and resource grants described in the
[library authorization prerequisites](../library/#core-authorization-prerequisites).

## Transfer behavior

IEC connection recovery runs immediately and then every 10 seconds after the
previous attempt completes, without a retry limit. An unavailable IEC server or
temporary DNS failure does not block connector startup. Closed or stopped
connections are replaced with a 10-second TCP-connect timeout and j60870's existing protocol timers;
successful connections perform the startup interrogation again. Recovery does
not depend on measurements changing. Shutdown stops further connection attempts.

For `external-to-core`, the IEC listener accepts short-float `M_ME_NC_1` and
`M_ME_TF_1` values only for configured inbound IOAs. It stamps them with the
connector's receipt time; the implementation does not retain the IEC timestamp
or quality flags. Non-finite values (`NaN` and positive/negative infinity) are
logged and discarded before queuing, without discarding valid readings.
Each poll merges the received queue into one in-memory pending
batch per IOA and attempts each batch independently. Failed submissions remain
pending for a later poll; they are lost if the connector process stops before a
successful retry.

For `core-to-external`, each poll reads the latest Core value within the shared
client's fixed 365-day search window and sends it as an `M_ME_NC_1` short float.
There is no sent-value checkpoint, so an unchanged latest value is sent again
on later polls. The connector is therefore a best-effort protocol bridge, not
an exactly-once queue.

Connection recovery does not backfill missed history or detect arbitrary stuck
workers. Pending measurements remain memory-only and can be lost on restart.
Pending data can grow without a bound during prolonged Core outages. Outbound
failures are isolated per IOA; a failing mapping does not skip the remaining ones.

## Run the image

```bash
CONFIG_DIR="${XDG_CONFIG_HOME:-$HOME/.config}/pegelhub/iec-connector"
docker run --rm \
  -v "${CONFIG_DIR}:/app/config:ro" \
  pegelhub-iec-connector:local
```

Replace the illustrative endpoints and credentials in an ignored copy before
running it against real systems. Core, Keycloak, and IEC addresses must be
reachable from inside the connector container.

For Compose-based deployments, use the
[shared connector runner](../../deploy/connector/).

## Protocol dependency

The implementation uses [OpenMUC j60870](https://www.openmuc.org/j60870/),
currently versioned in this module's `pom.xml`.
