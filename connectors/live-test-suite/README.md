# PegelHub Live Connector Server Suite

This module provides fake protocol servers and fake PegelHub Core/Keycloak endpoints for live connector checks.

## Running

Run the full suite from the repository root:

```sh
scripts/live-connector-suite.sh all
```

Run one protocol slice:

```sh
scripts/live-connector-suite.sh ftp
scripts/live-connector-suite.sh tstp
scripts/live-connector-suite.sh iec
scripts/live-connector-suite.sh icc
```

The script builds the harness and connector images, starts Docker Compose, runs the verifier, and exits with the verifier status. Successful runs attach only the verifier logs by default; failed runs print full Compose logs before cleanup.

Useful environment switches:

```sh
LIVE_VERIFY_TIMEOUT_SECONDS=120 scripts/live-connector-suite.sh all
LIVE_SUITE_NO_BUILD=1 scripts/live-connector-suite.sh ftp
LIVE_SUITE_VERBOSE=1 scripts/live-connector-suite.sh tstp
KEEP_LIVE_SUITE=1 scripts/live-connector-suite.sh iec
```

- `LIVE_VERIFY_TIMEOUT_SECONDS` changes the verifier polling timeout. The default is `90`.
- `LIVE_SUITE_NO_BUILD=1` skips image builds and reuses local images.
- `LIVE_SUITE_VERBOSE=1` streams every service log during the run. Without it, the script stays quiet unless the verifier fails.
- `KEEP_LIVE_SUITE=1` leaves containers running after a failure for inspection.
- `LIVE_SUITE_PROJECT_NAME` overrides the Compose project name when running multiple copies.

Run the smallest compile check for the harness without Docker:

```sh
mvn -B -ntp -f connectors/live-test-suite/pom.xml -DskipTests package
```

For harness-only debugging, start the fake servers locally and point the verifier at `localhost`:

```sh
java -cp 'connectors/live-test-suite/target/live-test-suite.jar:connectors/live-test-suite/target/lib/*' \
  at.pegelhub.connector.livetest.LiveTestSuiteApplication server

java -cp 'connectors/live-test-suite/target/live-test-suite.jar:connectors/live-test-suite/target/lib/*' \
  at.pegelhub.connector.livetest.LiveTestSuiteApplication verify ftp
```

## Scenarios

- `ftp`: ASC and ZRXP readers consume recent matching files and ignore stale or irrelevant fixture data.
- `tstp`: reader performs `Query` and `Get`; writer reads seeded Core data and sends `PUT`.
- `iec`: connector receives mapped IEC short-float measurements and writes mapped outbound values.
- `icc`: measurements are copied in both Core-to-external and external-to-Core directions with remapped time series IDs.

The suite intentionally uses tracked fixtures and fake endpoints so it does not depend on `old/` at runtime.
