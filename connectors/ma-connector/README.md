# mA connector

The mA connector reads raw 16-bit input values from a Revolution Pi process
image, maps piCtory variable names to Core time series, and writes those values
to Core. It supports input into Core only and requires the RevPi device at
runtime. Despite the connector name, the current implementation does not
convert raw values to milliamperes or apply calibration.

## Build

From the repository root:

```bash
mvn -B -ntp -pl connectors/ma-connector -am test
scripts/build-connector-image.sh ma-connector
```

The Maven build generates JNI headers. The multi-stage image build also
compiles the native `libRevPiReader.so` library and tags the result
`pegelhub-ma-connector:local`. Published images are built for `linux/arm64/v8`.

## Configure

The first command-line argument selects the configuration directory; containers
default to `/app/config`. The directory must contain `connector.yaml` and at
least one mapping YAML file. Use [`examples/config/`](examples/config/) as the
schema reference.

Create a private working copy outside the repository:

```bash
CONFIG_ROOT="${XDG_CONFIG_HOME:-$HOME/.config}/pegelhub"
install -d -m 700 "$CONFIG_ROOT"
test -d "$CONFIG_ROOT/ma-connector" || \
  cp -R connectors/ma-connector/examples/config "$CONFIG_ROOT/ma-connector"
chmod 700 "$CONFIG_ROOT/ma-connector"
chmod 600 "$CONFIG_ROOT/ma-connector/connector.yaml"
```

`connector.yaml` defines the Core URL and client-credentials authentication and
a positive polling interval ending in `s`, `m`, or `h` (case-insensitive).
`mappings.directory` defaults to `mappings`.

Each mapping names one piCtory input:

```yaml
revInput: "InputValue_1"
timeSeriesId: "11111111-1111-1111-1111-111111111111"
direction: "external-to-core"
```

Only `external-to-core` is accepted. Mapping files are loaded in sorted
filename order, and duplicate input names or resolved offsets fail startup.
The Keycloak client needs the `pegelhub-core-api` audience and the lowercase
Core role `measurement:write`.
It also needs the target source assignment described in the
[library authorization prerequisites](../library/#core-authorization-prerequisites).

## Read behavior

At startup, the native reader resolves configured piCtory variables to byte
offsets. Each mapped variable must be a byte-aligned 16-bit value: the reader
does not inspect piCtory's bit position or declared width. Each polling cycle
reads an unsigned, little-endian two-byte value from every offset in
`/dev/piControl0`. All values in a cycle receive the same connector timestamp
and are submitted to Core one at a time. A failed read or Core submission is
logged for that offset. It is not retained or retried; a later poll takes a new
sample with a new timestamp. There is no durable sample queue.

## Run on Revolution Pi

Provide a prepared configuration directory and the LAN address of the
Core/Keycloak host:

```bash
CONFIG_DIR="${XDG_CONFIG_HOME:-$HOME/.config}/pegelhub/ma-connector"
PEGELHUB_HOST_IP=192.0.2.10
docker run --rm \
  --device /dev/piControl0:/dev/piControl0 \
  --add-host "pegelhub-keycloak.test:${PEGELHUB_HOST_IP}" \
  -v "${CONFIG_DIR}:/app/config:ro" \
  pegelhub-ma-connector:local
```

Replace the documentation-only IP and edit `core.baseUrl` in the copied YAML to
an address reachable from the container. Also set its Keycloak token URL to the
`pegelhub-keycloak.test` hostname mapped by `--add-host`, then replace the
placeholder authentication values. The
checked-in [`examples/docker/docker-compose.yaml`](examples/docker/docker-compose.yaml)
shows the same device, host mapping, and read-only configuration mount for a
managed container. Adapt its image and config path for the target host. Real
credentials belong in an ignored directory, not in the image or repository.

For Compose-based deployments, use the
[shared connector runner](../../deploy/connector/), including its RevPi overlay.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| `open(/dev/piControl0) failed` | Device mapping, host permissions, and the piControl driver |
| `Short read: expected 2 bytes` | RevPi process image and piControl configuration |
| `Duplicate Input ...` | Keep one mapping per `revInput` |
| `Duplicate resolved offset ...` | Reconcile piCtory names that resolve to the same offset |
| Core receives no measurements | Core URL, issuer reachability, token audience/role, active connector registration, active metadata hierarchy, and matching source assignment |
