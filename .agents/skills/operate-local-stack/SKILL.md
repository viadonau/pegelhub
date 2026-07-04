---
name: operate-local-stack
description: Operate the repo's local Docker Compose stack. Use only to start, stop, restart, inspect Compose service status/logs, check actuator health, run read-only localhost smoke/API GET checks, or diagnose local Compose service startup/runtime failures.
---

Run from repo root:

```sh
scripts/local-stack.sh status
```

- `status`: compact first runtime snapshot.
- `compose-up`: build/start the local stack, then wait for actuator health.
- `compose-config`: validate Docker Compose config.
- `compose-ps`: show service status.
- `restart [core-app|meta-db|data-db|keycloak|keycloak-db]`: restart one service; default `core-app`.
- `logs-errors [service|all]`: show recent warning/error-looking lines; use before full logs.
- `logs [service|all]`: show targeted recent logs only when needed.
- `health`, `wait-health [seconds]`, `smoke [--raw]`, `api-get <path>`: low-risk runtime checks and read-only API GETs.
- `compose-down`: stop Compose containers while keeping volumes.

## Safety

- Do not remove Docker volumes unless explicitly asked.
