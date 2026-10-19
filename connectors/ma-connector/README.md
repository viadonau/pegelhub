# mA Connector

The mA connector reads raw 16-bit input values from a Revolution Pi process
image, maps piCtory variable names to Core time series, and writes those values
to Core. It supports input into Core only and requires the RevPi device at
runtime. Despite the connector name, the current implementation does not
convert raw values to milliamperes or apply calibration.

## Build and Test

Use JDK 21 and Maven 3.9 for tests, and Docker for the image build. Run from the
repository root:

```bash
mvn -B -ntp -pl connectors/ma-connector -am test
scripts/build-connector-image.sh ma-connector
```

The Maven build generates JNI headers. The multi-stage image build also
compiles the native `libRevPiReader.so` library and tags the result
`pegelhub-ma-connector:local`. Maven tests use a mocked device reader and do not
compile or exercise the native library. The image build skips tests, so run both
commands when validating a change.

The [image workflow](../../.github/workflows/images.yml) publishes mA for
`linux/arm64/v8` only. A local image build uses the Docker host platform unless
overridden; when building for an ARM64 RevPi on a different platform, pass
`--platform linux/arm64` to the build script. The native library and runtime
must match the target device's architecture.

## Configure

The first command-line argument selects the configuration directory; containers
default to `/app/config`. The directory must contain `connector.yaml` and at
least one mapping YAML file. Use [`examples/config/`](examples/config/) as the
schema reference. Configuration is loaded once at startup; restart after editing
it. YAML does not expand environment variables.

Create a private working copy outside the repository:

```bash
CONFIG_ROOT="${XDG_CONFIG_HOME:-$HOME/.config}/pegelhub"
install -d -m 700 "$CONFIG_ROOT"
test -d "$CONFIG_ROOT/ma-connector" || \
  cp -R connectors/ma-connector/examples/config "$CONFIG_ROOT/ma-connector"
chmod 700 "$CONFIG_ROOT/ma-connector"
chmod 600 "$CONFIG_ROOT/ma-connector/connector.yaml"
```

| Configuration key | Meaning |
| --- | --- |
| `core.baseUrl` | Core application root URL, with a trailing `/` |
| `core.authentication` | `tokenUrl`, `clientId`, and `clientSecret` for client-credentials access |
| `polling.interval` | Required positive duration, such as `30s`; accepts `s`, `m`, or `h` |
| `mappings.directory` | Mapping directory relative to the config root; defaults to `mappings` |

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

## Read Behavior

At startup, the native reader resolves configured piCtory variables to byte
offsets. A missing native library, inaccessible device, or unresolved variable
prevents startup. The reader always reads two bytes; it does not use the
variable's declared bit length or sign. Configure only compatible inputs and
verify their physical meaning before using these values as measurements.

The first poll starts one second after startup. Later polls start one
`polling.interval` after the previous job finishes. Each cycle reads an unsigned,
little-endian two-byte value (0-65,535) from every offset in `/dev/piControl0`.
All values in a cycle receive the same
connector timestamp and are submitted to Core one at a time. A failed read or
Core submission is logged for that offset. It is not retained or retried; a
later poll takes a new sample with a new timestamp. There is no durable sample
queue. A Core source representation can handle supported physical-unit
conversions, but it does not turn arbitrary raw counts into calibrated sensor
measurements. Do not assume the connector name means a 4-20 mA conversion exists.

## Run on Revolution Pi

Provide a prepared configuration directory with the published Core and Keycloak
HTTPS URLs reachable from the RevPi. The container needs access to the host
device:

```bash
CONFIG_DIR="${XDG_CONFIG_HOME:-$HOME/.config}/pegelhub/ma-connector"
docker run --rm \
  --device /dev/piControl0:/dev/piControl0 \
  -v "${CONFIG_DIR}:/app/config:ro" \
  pegelhub-ma-connector:local
```

For testing against the repository's local development stack on a different
machine, set `core.baseUrl` to `http://<development-host-ip>:8080/` and the full
token URL to
`http://pegelhub-keycloak.test:8082/realms/pegelhub/protocol/openid-connect/token`.
Add `--add-host "pegelhub-keycloak.test:<development-host-ip>"` to the Docker
command, replacing the IP placeholder. This HTTP setup is for a trusted local
test network only; do not use the `.test` name for a deployed HTTPS realm.

The checked-in
[`examples/docker/docker-compose.yaml`](examples/docker/docker-compose.yaml)
illustrates that local-development host mapping, device access, and read-only
configuration mount. Adapt its host, image, and config path before use. Real
credentials belong in a protected directory, not in the image or repository.

For Compose-based deployments, use the
[shared connector runner](../../deploy/connector/), including its RevPi overlay.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| `open(/dev/piControl0) failed` | Device mapping, host permissions, and the piControl driver |
| `UnsatisfiedLinkError` | Native library availability and matching CPU architecture |
| `KB_FIND_VARIABLE ioctl failed` | Exact piCtory variable name and deployed process-image configuration |
| `Short read: expected 2 bytes` | RevPi process image and piControl configuration |
| `Duplicate Input ...` | Keep one mapping per `revInput` |
| `Duplicate resolved offset ...` | Reconcile piCtory names that resolve to the same offset |
| Core receives no measurements | Core URL, issuer reachability, token audience/role, active connector registration, active metadata hierarchy, and matching source assignment |
