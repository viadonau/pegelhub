# Connector Compose Runner

This directory contains the shared Compose runner for the independently
deployed protocol connectors. The connector image supplies its own Java main
class; the runner supplies only the common container contract: configuration,
custom CA trust, restart behavior, and log rotation. It does not configure
protocol mappings, enroll a connector identity, or deploy Core.

Run the commands below from the repository root **on the connector host**. You
need Docker Engine, the Compose plugin, registry access to the intended image,
and network access to the published Core and Keycloak endpoints and the source
device or server.

## Prepare An Instance

Each connector instance is a directory outside the repository:

```text
/etc/pegelhub/connectors/<instance>/
  connector.env
  config/connector.yaml
  config/mappings/
  trust/
```

Have an administrator create the instance directory with ownership assigned to
the deployment account. Replace `my-connector` below with the instance name:

```sh
INSTANCE_DIR=/etc/pegelhub/connectors/my-connector
mkdir -p "$INSTANCE_DIR/config/mappings" "$INSTANCE_DIR/trust"
chmod 700 "$INSTANCE_DIR/config"
test -f "$INSTANCE_DIR/connector.env" || \
  cp deploy/connector/connector.env.example "$INSTANCE_DIR/connector.env"
chmod 600 "$INSTANCE_DIR/connector.env"
```

Review [`connector.env.example`](connector.env.example) and set these values in
the installed `connector.env`:

| Key | Purpose |
| --- | --- |
| `COMPOSE_PROJECT_NAME` | A unique, stable project name for this instance. |
| `PEGELHUB_CONNECTOR_IMAGE` | The protocol-specific image at a published commit/release tag or digest; do not use `latest`. |
| `PEGELHUB_TRUST_MODE` | `system` (default) or `custom`. |
| `CONNECTOR_JAVA_TOOL_OPTIONS` | Optional JVM settings, passed as `JAVA_TOOL_OPTIONS`. |

Image names are `ghcr.io/viadonau/pegelhub-<module>`, for example
`ghcr.io/viadonau/pegelhub-ftp-connector:sha-42bd19b`. Select a published tag or
digest rather than using this illustrative value unchanged. Core and the
connector image do not have to share a release tag.

Create `config/connector.yaml` and its mapping files from the relevant module's
examples. Replace placeholder URLs, credentials, and metadata IDs before
starting the container:

- [FTP connector](../../connectors/ftp-connector/README.md)
- [ICC connector](../../connectors/icc-connector/README.md)
- [IEC 60870-5-104 connector](../../connectors/iec-connector/README.md)
- [mA connector](../../connectors/ma-connector/README.md)
- [TSTP connector](../../connectors/tstp-connector/README.md)

Keep protocol credentials and the enrolled connector's client credentials in
the protected instance configuration, never in Git. Identity enrollment and
matching Core metadata must be completed before the connector can publish data.

## Validate And Start

The Compose file resolves `./config` and `./trust` relative to the instance
directory through `--project-directory`. Validate the Compose configuration
without pulling an image or starting services:

```sh
docker compose \
  --project-directory "$INSTANCE_DIR" \
  --env-file "$INSTANCE_DIR/connector.env" \
  -f deploy/connector/compose.yaml \
  config --quiet
```

This checks Compose syntax and interpolation, not image availability,
connector YAML, credentials, or protocol connectivity. After reviewing the
configuration, start the instance:

```sh
docker compose \
  --project-directory "$INSTANCE_DIR" \
  --env-file "$INSTANCE_DIR/connector.env" \
  -f deploy/connector/compose.yaml \
  up -d --pull always
```

Use the same options with `ps` or `logs --tail=100 connector` to inspect it,
and with `stop connector` to stop it. Containers restart unless stopped, and
the JSON log driver keeps at most five 10 MB files. There is no container
healthcheck in this runner: a running container alone does not confirm that
measurements reached Core.

## RevPi Hardware

The mA connector additionally needs the RevPi device overlay, which maps
`/dev/piControl0` from the host. The device must exist and the host must have
the required RevPi hardware and driver. Published mA images target Linux ARM64;
the other connector images target Linux AMD64 and ARM64.

```sh
docker compose \
  --project-directory "$INSTANCE_DIR" \
  --env-file "$INSTANCE_DIR/connector.env" \
  -f deploy/connector/compose.yaml \
  -f deploy/connector/revpi.compose.yaml \
  up -d --pull always
```

Include the overlay in subsequent validation and lifecycle commands for that
instance as well.

## Network And Trust

The runner does not join the Core platform network. Connector configuration
must therefore use the published Core and Keycloak FQDNs, including when the
connector runs on the same host as the platform.

The config and trust directories are mounted read-only at `/app/config` and
`/run/pegelhub/extra-ca`. An empty `trust/` directory is valid in `system` mode.
For `custom` mode, add individual private CA certificates with a `.crt` suffix;
see [Java container trust](../../docker/README.md). Restart the connector after
changing CA files, or recreate it when changing its environment or image.

To update the connector, set the intended image in `connector.env` and rerun
the start command. To roll back, restore the prior image reference and recreate
the container. This runner does not record release history or automatically
roll back failed deployments. The GitHub staging workflow supplies the FTP
image through an environment override; it does not persist that reference to
the instance's `connector.env`.
