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
positive polling interval ending in `s`, `m`, or `h` (case-insensitive), and
the IEC server `host`, `port`, and `commonAddress`. The common address is used
for the startup interrogation and outbound ASDUs. Inbound values are selected
by configured IOA; the current listener does not filter them by ASDU common
address. `mappings.directory` defaults to `mappings`.

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

Core authentication must produce a token for the `pegelhub-core-api` audience
with the direction-appropriate lowercase role, such as `measurement:write` for
inbound values or `measurement:read` for outbound values.
The client also needs the registration and resource grants described in the
[library authorization prerequisites](../library/#core-authorization-prerequisites).

## Transfer behavior

The connector performs no unit conversion. For `external-to-core`, configure
the target time series source representation to match the physical IEC value:
normally `canonical`, or `metres-above-adria` only when a water-level value is
an elevation and the measuring point has a gauge zero. For `core-to-external`,
Core returns canonical values, so the IEC destination must expect the observed
property's canonical unit.

For `external-to-core`, the IEC listener accepts short-float `M_ME_NC_1` and
`M_ME_TF_1` values only for configured inbound IOAs. It stamps them with the
connector's receipt time; the implementation does not retain the IEC timestamp
or quality flags. Each poll merges the received queue into one in-memory pending
batch per IOA and attempts each batch independently. Failed submissions remain
pending for a later poll; they are lost if the connector process stops before a
successful retry.

For `core-to-external`, each poll reads the latest Core value within the shared
client's fixed 365-day search window and sends it as an `M_ME_NC_1` short float.
There is no sent-value checkpoint, so an unchanged latest value is sent again
on later polls. The connector is therefore a best-effort protocol bridge, not
an exactly-once queue.

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

The implementation uses
[OpenMUC j60870](https://www.openmuc.org/iec-60870-5-104/), currently versioned
in this module's `pom.xml`.
