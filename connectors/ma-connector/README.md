# mA Connector

The mA connector reads milliampere-based input signals from a Revolution Pi, converts them into Pegelhub measurements, and forwards them to Pegelhub Core.

## Build

```sh
mvn -pl connectors/ma-connector -am -DskipTests package
```

The build also generates the JNI headers used by the native RevPi binding.

## Configuration

The connector accepts an optional first CLI argument pointing to the config directory. Without an argument it reads from `/app/config`.

The config directory must contain `connector.yaml` and a `mappings/` directory.

`connector.yaml`:

```yaml
core:
  baseUrl: "http://localhost:8080/"
  authentication:
    tokenUrl: "http://localhost:8082/realms/pegelhub/protocol/openid-connect/token"
    clientId: "connector"
    clientSecret: "secret"
polling:
  interval: "30s"
mappings:
  directory: "mappings"
```

Each mapping file defines one RevPi input by its piCtory variable name:

```yaml
revInput: "InputValue_1"
timeSeriesId: "11111111-1111-1111-1111-111111111111"
direction: "external-to-core"
```

mA mappings only support `external-to-core`.

## Docker Compose

An example compose file is checked in at `examples/docker/docker-compose.yaml`.

```yaml
services:
  ma-connector:
    image: ${MA_CONNECTOR_IMAGE:?Set MA_CONNECTOR_IMAGE}
    restart: unless-stopped
    devices:
      - "/dev/piControl0:/dev/piControl0"
    volumes:
      - ./config:/app/config:ro
    environment:
      JAVA_TOOL_OPTIONS: "-DLOG_LEVEL=INFO"
```

When Core and Keycloak run on another machine in the same LAN, add a host mapping for the Keycloak hostname used by `connector.yaml`.

## Common Problems

| Problem                         | Likely Cause                            | Action                                                                               |
|---------------------------------| --------------------------------------- | ------------------------------------------------------------------------------------ |
| `open(/dev/piControl0) failed`  | Device not mapped or permissions        | Map device in Docker and check `ls -l /dev/piControl0`                               |
| `Short read: expected 2 bytes`  | RevPi process image not available       | Verify piControl driver and RevPi config                                             |
| `Duplicate Input <name>`        | Same `revInput` in multiple files       | Keep one mapping per input                                                           |
| `Duplicate resolved offset <n>` | Two names map to same offset            | Reconcile piCtory variable names                                                     |
| No measurements arrive at Core  | Core unreachable or authentication invalid | Verify `core.baseUrl`, Core authentication, and network routing                   |
