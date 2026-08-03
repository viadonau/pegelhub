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
other direction is accepted. `parameter` is optional and is used by ZRXP
parsing. Mapping files are loaded in sorted filename order, although FTP
requires exactly one.

The Keycloak client needs a token for the `pegelhub-core-api` audience and the
Core role `measurement:write`. Register the same client ID as Connector
metadata in Core before ingesting measurements. The staging identity policy
also grants `telemetry:write`; this connector currently submits measurements.
Core also requires the target time series source binding and `WRITE` grant
described in the [library authorization prerequisites](../library/#core-authorization-prerequisites).

## Run the image

```bash
CONFIG_DIR="${XDG_CONFIG_HOME:-$HOME/.config}/pegelhub/ftp-connector"
docker run --rm \
  -v "${CONFIG_DIR}:/app/config:ro" \
  pegelhub-ftp-connector:local
```

Edit the copied schema before running. Its placeholder authentication and FTP
values are not usable credentials. Never commit real client or FTP secrets.

Staging runs this connector inside the supported Compose topology. Its
server-local configuration and manual Keycloak enrollment are documented in
the [staging guide](../../deploy/staging/#ftp-connector-configuration).
