---
name: pegelhub-local-dev
description: Operate the PegelHub local development environment for the Spring Boot core module under `core/`. Use when Codex needs to build or test core, initialize or validate local `.env` config, start or inspect Docker Compose services, check actuator health, run safe read-only API smoke checks, or diagnose local Postgres, InfluxDB, or `core-app` startup issues.
---

# PegelHub Local Dev

Use the helper first. It resolves repo paths for the current `core/` layout, protects `.env` contents, validates Compose config without printing resolved secrets, handles `docker compose`/`docker-compose`, and keeps smoke checks read-only.

## Command Surface

Run from the repository root:

```sh
bash .agents/skills/pegelhub-local-dev/scripts/pegelhub-local-dev.sh doctor
```

- `status`: compact first diagnostic pass; use before `doctor` when a low-token snapshot is enough.
- `doctor`: deeper diagnostic pass; checks Java/Maven/Docker, local `.env`, git status, Compose status, and actuator reachability.
- `init-env`: copy the core module `.env.example` to `.env` if missing. Use only for local defaults; never print `.env`.
- `env-check`: compare `.env.example` keys against `.env` without printing values.
- `test-core`: run core tests with `mvn -f core/pom.xml -DfailIfNoTests=false test`.
- `build-core`: package the core with tests skipped.
- `compose-up`: validate config, build/start `core-app`, `meta-db`, `data-db`, then wait for actuator health.
- `compose-up-deps`: validate config and start only `meta-db` and `data-db` for IDE/Maven app runs.
- `compose-ps`, `compose-config`, `restart [service]`, `compose-down`: inspect or control the stack. `compose-down` keeps volumes.
- `logs [core-app|meta-db|data-db|all]`: targeted logs, default `core-app`; set `TAIL=300` when needed.
- `logs-errors [core-app|meta-db|data-db|all]`: recent warning/error-looking lines only; use before dumping full logs.
- `health`, `wait-health [seconds]`, `smoke [--raw]`, `api-get <path>`: low-risk runtime checks. Default `smoke` prints compact reachability; `smoke --raw` prints payloads.

Override `PEGELHUB_REPO_ROOT`, `CORE_BASE_URL`, `ACTUATOR_BASE_URL`, or `TAIL` only when the defaults do not match the current environment.

## Operating Pattern

1. Start with `status` before edits or runtime changes; use `doctor` when the compact status leaves a question.
2. For local stack issues, run `status`, then `compose-ps`, then `logs-errors`, then targeted `logs`; avoid broad log dumps until needed.
3. For app code changes, run the narrow relevant tests first, then `test-core` or `build-core` depending on risk and time.
4. For runtime verification, prefer `health`, `smoke`, and explicit `api-get` reads before any write/send endpoint.
5. If a command fails because the environment is not running, inspect service status and logs before changing application code.

## Local Runtime Facts

- Core API: `http://localhost:8080/api/v1/**`
- Actuator: `http://localhost:8081/actuator`
- Compose services: `core-app`, `meta-db`, `data-db`
- Host ports: core `8080`, actuator `8081`, Postgres `5444`, InfluxDB `8111`
- Local config file: `core/.env` in the current repo layout
- Local defaults template: `core/.env.example`
- Compose file: `core/docker-compose.yaml`
- InfluxDB details: `core/docs/influxdb.md`
- API client docs: `core/docs/api/postman/`

## Safety

- Do not print `.env` contents unless the user explicitly asks to inspect local config; summarize presence/missing keys instead.
- Do not remove Docker volumes unless the user explicitly approves data loss.
- Treat `docker compose down -v`, database cleanup, and volume removal as destructive.
- Do not run connector write/send smoke tests by default; connector-to-core checks may create measurements.
- Keep secrets and local credentials out of prompts, notes, scripts, and final answers.
- Summarize large JSON or local data payloads in final answers unless the user asks for raw output.

## Troubleshooting

- If `core-app` first logs a refused Postgres/Influx connection but later starts cleanly, inspect service health and restart timing before changing application code.
- If `meta-db` reports `initdb: directory exists but is not empty`, suspect a bad or incompatible Docker volume state.
- If `data-db` bucket creation logs show an initial 404 followed by created bucket output, that can be normal for create-if-missing scripts.
- If Influx auth fails after `.env` changes, the existing Influx volume may have been initialized with a different token; update `.env` to match or ask before recreating volumes.
- If connector containers fail with `UnsupportedClassVersionError`, verify they use Java 21 images.
