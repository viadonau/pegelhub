# PegelHub Live Connector Server Suite

This module provides fake protocol servers and fake PegelHub Core/Keycloak endpoints for live connector checks.

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

The script builds the harness and connector images, starts Docker Compose, runs the verifier, and exits with the verifier status. On failure it prints Compose logs. Set `KEEP_LIVE_SUITE=1` to leave the containers running after a failure.

## Scenarios

- `ftp`: ASC and ZRXP readers consume recent matching files and ignore stale or irrelevant fixture data.
- `tstp`: reader performs `Query` and `Get`; writer reads seeded Core data and sends `PUT`.
- `iec`: connector receives mapped IEC short-float measurements and writes mapped outbound values.
- `icc`: measurements are copied in both Core-to-external and external-to-Core directions with remapped time series IDs.

The suite intentionally uses tracked fixtures and fake endpoints so it does not depend on `old/` at runtime.
