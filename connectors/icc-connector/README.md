# ICC Connector

The ICC connector periodically copies recent time-series measurements between
a local PegelHub Core instance and a remote PegelHub Core instance. Each mapping
chooses its direction independently. ICC transfers measurement values and
observation timestamps only; it does not replicate metadata, source identities,
access grants, or telemetry.

## Build and Test

Use JDK 21 and Maven 3.9 for tests, and Docker for the image build. Run from the
repository root:

```bash
mvn -B -ntp -pl connectors/icc-connector -am test
scripts/build-connector-image.sh icc-connector
```

The image is tagged `pegelhub-icc-connector:local`. The image build compiles the
application itself but skips tests, so run both commands when validating a change.

## Configure

The first command-line argument selects the configuration directory; containers
default to `/app/config`. The directory must contain `connector.yaml` and at
least one mapping YAML file. Start from [`examples/config/`](examples/config/)
and replace its illustrative hosts and credentials. Configuration is loaded once
at startup; restart after editing it. YAML does not expand environment variables.

Create a private working copy outside the repository:

```bash
CONFIG_ROOT="${XDG_CONFIG_HOME:-$HOME/.config}/pegelhub"
install -d -m 700 "$CONFIG_ROOT"
test -d "$CONFIG_ROOT/icc-connector" || \
  cp -R connectors/icc-connector/examples/config "$CONFIG_ROOT/icc-connector"
chmod 700 "$CONFIG_ROOT/icc-connector"
chmod 600 "$CONFIG_ROOT/icc-connector/connector.yaml"
```

| Configuration key | Meaning |
| --- | --- |
| `localCore`, `remoteCore` | Each requires a Core application-root `baseUrl` ending in `/` and `authentication.tokenUrl`, `clientId`, and `clientSecret` |
| `polling.interval` | Required positive duration, such as `15m`; accepts `s`, `m`, or `h` |
| `polling.overlap` | Positive replay duration in the same format; defaults to `1h` |
| `mappings.directory` | Mapping directory relative to the config root; defaults to `mappings` |

### Mappings and Access

Each mapping relates one local and one remote Core time series:

```yaml
timeSeriesId: "11111111-1111-1111-1111-111111111111"
externalTimeSeriesId: "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
direction: "core-to-external"
```

- `core-to-external` reads `timeSeriesId` from local Core and writes
  `externalTimeSeriesId` to remote Core.
- `external-to-core` reads `externalTimeSeriesId` from remote Core and writes
  `timeSeriesId` to local Core.

Mapping files are processed in sorted filename order. Configure each Keycloak
client for the `pegelhub-core-api` audience and only the read or write roles
needed on that side, using the lowercase runtime values such as
`measurement:read` and `measurement:write`.
Each Core client also needs the registration and resource grants described in
the [library authorization prerequisites](../library/#core-authorization-prerequisites).

Both time series must already exist with compatible properties and canonical
units. ICC reads canonical values and submits them unchanged, so the destination
source assignment must use `canonical` and identify the ICC client registered
on that Core instance. The original source connector is not preserved as the
destination writer. ICC does not validate cross-instance metadata compatibility
or reject duplicate/cyclic mapping routes; review the whole route before enabling
transfers.

## Transfer Behavior

The first cycle starts immediately. Later cycles start one `polling.interval`
after the previous cycle finishes. One worker processes mappings serially,
using the same cycle end time for all mappings.

Both directions reread a recent overlap so readings arriving after an earlier
successful poll can still be delivered. Optional `polling.overlap` defaults to
`1h` and uses the same positive `s`, `m`, or `h` literals as `polling.interval`.
The first cycle reads `[cycle time - polling interval - overlap, cycle time)`.
After success, including an empty result, the next start is the cycle time
minus overlap, unless the previous start is later. The next read ends at its
new cycle time. This covers scheduler processing time without delaying newly
available data.

The entire source window is resent with the target time-series ID and original
observation timestamps. Keep the writing connector identity stable: repeated
writes update the same Core measurement identity, including its receipt time.

Mapping failures are logged independently so later mappings still run. A
failed mapping keeps its previous start boundary and retries the enlarged
window on the next cycle. An interrupted worker stops before starting another
mapping. Boundaries exist only in process memory: restarting reads one polling
interval plus overlap, not the previous process's checkpoint.
Late readings older than the overlap can still be missed after successful
polls. Size overlap for observation-to-source-visibility delay at each hop,
including all upstream polling and processing. One hour allows margin for
5-15-minute upstream polling; hourly upstream polling needs a larger downstream
overlap plus margin for delays (for example, `2h`). This is a sizing example,
not a delivery guarantee.
There is no historical-backfill service, durable checkpoint, or exactly-once
guarantee.

## Run the Image

```bash
CONFIG_DIR="${XDG_CONFIG_HOME:-$HOME/.config}/pegelhub/icc-connector"
docker run --rm \
  -v "${CONFIG_DIR}:/app/config:ro" \
  pegelhub-icc-connector:local
```

The checked-in configuration is a schema example, not a working environment.
Both Core and Keycloak addresses must be reachable from inside the connector
container. Keep real client secrets in an ignored, read-only mounted directory.

For Compose-based deployments, use the
[shared connector runner](../../deploy/connector/).
