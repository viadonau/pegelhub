# mA Connector
## 1. Summary
The Pegelhub mA Connector reads milliampere-based input signals from a Revolution Pi (RevPi), 
converts them into Pegelhub measurements, and forwards them to Pegelhub Core over HTTP.
It runs as a Docker container that periodically polls the configured RevPi inputs and 
sends the latest measurements.

## 2. Folder Structure
```
ma-connector/
├── src/
│   ├── main/java/…               # Java sources
│   ├── main/native/…             # C++ JNI binding + headers
│   └── main/resources/…          # Runtime resources
├── examples/
│   ├── config/…                  # Sample connector.properties
│   ├── data/inputs/…             # Sample input YAMLs
│   └── docker/…                  # Compose example
├── Dockerfile                    # Multi-stage build (native + runtime)
└── pom.xml                       # Maven build configuration
````

## 3. Architecture Overview

- **MaConnectorApplication**  
  Bootstraps the connector: loads config, initializes JNI, loads inputs, and starts the scheduler.
- **MaConfigLoader / MaConnectorOptions**  
  Parses `connector.properties`, validates required fields, and exposes runtime options (core target, delay, inputs directory).
- **InputRegistry**  
  Scans the `InputsDir` for YAML files, reads the `revInput` name, resolves it to a RevPi process image offset, and creates a PegelHubCommunicator per input.
- **RevPiReader (JNI)**  
  Native C++ implementation reads from `/dev/piControl0`. Exposed to Java via `RevPiReaderImpl`.
- **MaReadJob**  
  Reads values for each registered input, creates a `Measurement`, and sends it via the mapped `PegelHubCommunicator`.
- **MaConnectorScheduler**  
  Triggers `MaReadJob` with a fixed delay.

## 4. Configuration
### 4.1 `connector.properties`

The connector accepts an optional first CLI argument pointing to the config directory.
Without an argument it reads from `/app/config/connector.properties`.

| Key             | Type   | Example             | Notes                                        |
| --------------- |--------| ------------------- | -------------------------------------------- |
| `Core.IP`       | String | `192.168.2.29`      | Hostname/IP of Pegelhub Core                 |
| `Core.Port`     | Int    | `8081`              | Port of Pegelhub Core                        |
| `DelayInterval` | String | `30s`, `2m`, `1h`   | Case-insensitive `s/m/h`, whole numbers only |
| `InputsDir`     | String | `/app/data/inputs`  | Directory containing YAML input files        |

**Sample:**

```properties
Core.IP=192.168.2.29
Core.Port=8081
DelayInterval=30s
InputsDir=/app/data/inputs
```

### 4.2 Inputs (YAML)
Each metadata YAML file defines one RevPi input by its variable name from piCtory.

```yaml
# /app/data/inputs/mA_input_1.yaml
revInput: "InputValue_1"
keycloak:
  tokenUrl: "http://pegelhub-keycloak.test:8082/realms/pegelhub/protocol/openid-connect/token"
  clientId: "local-connector-example"
  clientSecret: "local-dev-connector-secret-change-me"
sendMetaDataOnStartup: false
isSupplier: true
supplier:
  id: 30
  name: "IecConnector30"
...
```

## 5. Deployment
### 5.1 Prerequisites

* docker and docker-compose installed
* RevPi equipped with an AIO (Analog I/O) module and connected sensor(s)
* PiCtory configured for the sensor’s AIO input: set the correct multiplier, divisor, and input offset so values are converted correctly
* Device access to `/dev/piControl0` for the container
* Network connectivity to the Pegelhub Core

### 5.2 Get the image
Option A - use the prebuilt image:

```bash
docker pull markusf01/pegelhub-ma-connector:latest
```

Option B - build from source and load on the RevPi:

```bash
# From the repository root on your build machine
mvn -pl connectors/ma-connector -am -DskipTests package

# The Java build produces:
# - connectors/ma-connector/target/ma-connector.jar
# - connectors/ma-connector/target/lib/*.jar
# - connectors/ma-connector/target/generated-sources/jni/*.h

# Build the container image from the connector directory
cd connectors/ma-connector
docker buildx build --platform linux/arm64/v8 --load -t ma-connector .
docker save -o ma-connector.tar ma-connector

# Transfer to the RevPi (example using scp; a USB stick works too)
scp ./ma-connector.tar pi@192.168.10.10:/home/pi/

# On the RevPi
docker load -i /home/pi/ma-connector.tar
```

### 5.3 Docker Compose

Create a `docker-compose.yaml` on the RevPi. If you use the prebuilt image, set `image: markusf01/pegelhub-ma-connector:latest`. If you loaded a locally built image, set `image: ma-connector`.
An example file is checked in at `examples/docker/docker-compose.yaml`.

```yaml
version: "2.2"
services:
  ma-connector:
    image: markusf01/pegelhub-ma-connector:latest
    extra_hosts:
      - "pegelhub-keycloak.test:<Mac LAN IP>"
    devices:
      - "/dev/piControl0:/dev/piControl0"
    volumes:
      - ./config:/app/config:ro
      - ./data:/app/data:ro
    environment:
      JAVA_TOOL_OPTIONS: "-DLOG_LEVEL=INFO"
```

### 5.4 Configure files

Prepare the connector config and input YAML files on the host (same directory as your `docker-compose.yaml`):

```bash
mkdir -p config data/inputs
cp ./your-connector.properties config/connector.properties
cp ./your-inputs/*.yaml      data/inputs/
```

`connector.properties` contains connector runtime settings such as `Core.IP`, `Core.Port`,
`DelayInterval`, and `InputsDir`. Each input YAML contains the RevPi variable name
(`revInput`) plus the PegelHub/Keycloak client settings used by that input. The mA connector
does not use a separate `pegelhub.yaml`; checked-in examples live under `examples/config/`
and `examples/data/inputs/`.

### 5.5 Start

```bash
# Start in the background
docker compose up -d
```

### 5.5 Stop

```bash
# Stop
docker compose down
```

### 5.6 Logs

```bash
docker compose logs -f ma-connector
```

Logging is handled by Logback and printed to the container’s console. Adjust the level using:

```bash
# Example: set DEBUG
JAVA_TOOL_OPTIONS="-DLOG_LEVEL=DEBUG"
```


## 6 Possible Problems

| Problem                         | Likely Cause                            | Action                                                                               |
|---------------------------------| --------------------------------------- | ------------------------------------------------------------------------------------ |
| `open(/dev/piControl0) failed`  | Device not mapped / permissions         | Map device in Docker; check `ls -l /dev/piControl0`; run with appropriate privileges |
| `Short read: expected 2 bytes`  | RevPi process image not available/ready | Verify piControl driver, RevPi config, power-cycle if needed                         |
| `Duplicate Input <name>`        | Same `revInput` in multiple files       | Keep one; remove/rename duplicates                                                   |
| `Duplicate resolved offset <n>` | Two names map to same offset            | Keep one; reconcile piCtory variable names                                           |
| `Unknown unit: x`               | Bad `DelayInterval`                     | Use `Xs`, `Xm`, `Xh` with integers                                                   |
| No measurements arrive at Core  | Core unreachable or wrong IP/Port       | Verify `Core.IP`/`Core.Port`; network routing/firewall                               |
