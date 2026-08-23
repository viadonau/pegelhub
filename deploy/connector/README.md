# Connector Compose runner

This directory contains the shared Compose runner for the independently
deployed protocol connectors. The connector image supplies its own Java main
class; the runner supplies only the common container contract: configuration,
custom CA trust, restart behavior, and log rotation.

Each connector instance is a directory outside the repository:

```text
/etc/pegelhub/connectors/<instance>/
  connector.env
  config/connector.yaml
  config/mappings/
  trust/
```

Copy [`connector.env.example`](connector.env.example) into the instance as
`connector.env`, then set a unique project name and the connector image.

`connector.env` contains the project name, immutable connector image, trust
mode, and optional Java settings. The Compose file resolves `./config` and
`./trust` relative to the instance directory through `--project-directory`:

```sh
INSTANCE_DIR=/etc/pegelhub/connectors/<instance>
docker compose \
  --project-directory "$INSTANCE_DIR" \
  --env-file "$INSTANCE_DIR/connector.env" \
  -f deploy/connector/compose.yaml \
  up -d --pull always
```

The config and trust directories are mounted read-only. An empty `trust/`
directory is valid in `system` mode; company CA certificates are placed there
when `PEGELHUB_TRUST_MODE=custom` is selected.

Validate a rendered configuration without opening the protocol, Core, or RevPi
connections. This is safe while the corresponding legacy connector is still
running:

```sh
docker compose \
  --project-directory "$INSTANCE_DIR" \
  --env-file "$INSTANCE_DIR/connector.env" \
  -f deploy/connector/compose.yaml \
  run --rm -e PEGELHUB_VALIDATE_CONFIG=true connector
```

Do not start the V2 connector service during rehearsal. At cutover, verify the
legacy connector is stopped before starting its replacement.

The mA connector additionally needs the RevPi device overlay:

```sh
docker compose \
  --project-directory "$INSTANCE_DIR" \
  --env-file "$INSTANCE_DIR/connector.env" \
  -f deploy/connector/compose.yaml \
  -f deploy/connector/revpi.compose.yaml \
  up -d --pull always
```

The runner does not join the Core platform network. Connector configuration
must therefore use the published Core and Keycloak FQDNs, including when the
connector runs on the same host as the platform.
