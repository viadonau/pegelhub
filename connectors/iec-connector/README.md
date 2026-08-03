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

Example mapping:

```yaml
iecIoa: 66049
timeSeriesId: "11111111-1111-1111-1111-111111111111"
direction: "external-to-core"
```

Use `external-to-core` for IEC values written to Core and `core-to-external`
for Core measurements written to IEC. Mapping files are loaded in sorted
filename order.

Core authentication must produce a token for the `pegelhub-core-api` audience
with the direction-appropriate lowercase role, such as `measurement:write` for
inbound values or `measurement:read` for outbound values.
The client also needs the registration and resource grants described in the
[library authorization prerequisites](../library/#core-authorization-prerequisites).

## Run the image

```bash
CONFIG_DIR="${XDG_CONFIG_HOME:-$HOME/.config}/pegelhub/iec-connector"
docker run --rm \
  -v "${CONFIG_DIR}:/app/config:ro" \
  pegelhub-iec-connector:local
```

Replace the illustrative endpoints and credentials in an ignored copy before
running it against real systems.

## Protocol dependency

The implementation uses [OpenMUC j60870](https://www.openmuc.org/j60870/),
currently versioned in this module's `pom.xml`.
