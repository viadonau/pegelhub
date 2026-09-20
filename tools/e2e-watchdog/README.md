# PH Return-Series Watchdog

A small, independently deployed Java 21 process watches the age of **one existing return series**
and sends SNMP ERR/OK events. It reads Core directly, without QA or notifications. There is no
generator, measurement write, frontend, HTTP server, broker, or external database.
The existing `e2e-watchdog` artifact/image name is retained. Nothing here deploys production monitoring.

## Code Layout

Packages under `src/main/java/at/pegelhub/watchdog` are grouped by responsibility:

```text
WatchdogApplication.java     CLI entry point, sequential loop, and wiring
Json.java                    Shared state/diagnostic JSON encoding
config/
  WatchdogConfig.java        Validated settings, route identity, and secret-file access
  ConfigurationFailure.java Safe configuration errors for the CLI
monitoring/
  Watchdog.java              Read Core and record the evaluated result
  Checks.java                Pure freshness rules
  CheckState.java            Result and ERR/OK mapping
state/
  StateStore.java            Durable watchdog state, restart rules, and health
  SqliteStateStorage.java    Package-private SQLite and file lifecycle
snmp/
  SnmpPublisher.java         Event publication and receiver retries
```

Tests mirror `monitoring`, `state`, and `snmp`. Their shared configuration lives in
`WatchdogFixtures`; protocol simulators remain test-only under `lab`.

## Exact Behavior

| Observation after a completed read | Local status / reason | SNMP signal |
| --- | --- | --- |
| Latest timestamp is no older than the configured limit | `OK / fresh` | OK |
| Latest timestamp is older than the limit | `CRITICAL / stale` | ERR |
| Series contains no measurement | `CRITICAL / no_measurement` | ERR |
| Core/authentication/response parsing fails | `UNKNOWN / read_failed` | ERR |
| Timestamp is in the future, or the watchdog clock regresses | `UNKNOWN / future_timestamp` or `clock_regressed` | ERR |

Age is `evaluation time - observedAt`, not time since the last successful HTTP request.
Re-reading the same point cannot reset its age. A constant value with a new timestamp is healthy.
The exact age limit is still OK; the first completed check beyond it is ERR. There is no
consecutive-failure count. Default polling is every 15 seconds **after** the previous complete cycle
(read, evaluation, publication, health update). Network time adds to detection time.
HTTP connect/response limits are 5/10 seconds.

A successful fresh read is the only recovery evidence. Failed reads never give an OK or
acknowledge an existing error. A freshly read old point remains stale. Unlike the former synthetic
check, there is no per-probe deadline or requirement for a probe generated after the failure.

### What This Proves

When the return series has a single authorized writer and can only receive data through the agreed
complete route, sufficiently recent timestamps are evidence of recent route activity.
Choose a gauge that really emits regularly even when its value does not change, and include
all connector polling/transport delays in the maximum age.

This does **not** detect individual lost samples, changed values, duplicates, or a bypass of the route.
Fresh-but-wrong values are deliberately accepted: this is availability monitoring, not a quality or
integrity check. Old replayed data is detected only if its observation timestamps are preserved.
Receipt-stamped IEC data proves activity at that receiver, not necessarily the sensor's measurement age.
IEC timestamp handling is unchanged.

## Configuration and Access

Start from [examples/watchdog.yaml](examples/watchdog.yaml). The age limit is deliberately zero
and invalid until agreed explicitly. Ten minutes means `silenceSeconds: 600`, but is only an example;
it is unsuitable if the route itself polls every 15 minutes. Allowed age: 1 second through 7 days;
poll interval: 1 through 60 seconds.

Use an administrator only for provisioning through the existing supported APIs:

1. Create a confidential Keycloak service client with Core audience `pegelhub-core-api`,
   actor type `CLIENT`, and **only `measurement:read`**. No administrator or measurement-write role.
2. Register it with `POST /api/v1/admin/connectors`, `connector.type: other`.
   Registration is the existing API authorization mechanism, not a new ingestion connector.
3. Grant access only to the monitored return series through
   `PUT /api/v1/connectors/{connectorId}/read-access/time-series/{seriesId}`.
   Do not give the watchdog a source assignment.
4. Separately configure the gauge's outgoing/return route and its exclusive writers.

[Lab bootstrap](lab/bootstrap.py) demonstrates this with real Keycloak and Core.
The watchdog uses the shared client's latest read:
`GET /api/v1/time-series/{id}/measurements?last=365d&order=desc&limit=1&representation=canonical`.
Existing connector client defaults and Core behavior remain unchanged.

Mount configuration and secret files read-only, readable by UID 10001. Secrets are read at startup,
never written to SQLite or returned in status. Restart after changing configuration or rotating secrets.

## State and Runtime

One loop on the main thread reads Core, evaluates and persists the result, publishes any required
ERR/OK event, then waits for the configured poll interval. A failed Core read becomes `read_failed`
and still reaches publication in that cycle. An unexpected storage/runtime failure stops the process
visibly. Shutdown wakes the poll wait or lets the in-flight cycle finish before closing resources.

There is no separate publisher timer. Slow reads delay publication and retries; slow sends delay the
next read. Network operations stay outside short SQLite transactions, so diagnostics remain readable.
The loop records one local health heartbeat only after a completed cycle and at most once every
30 seconds, using monotonic elapsed time. A stuck read or send cannot renew it. This heartbeat proves
loop progress, not route health, and does not send SNMP packets. Alarm and engine-state changes are
persisted immediately.

SQLite contains only bounded current state: latest check, clock watermark, loop progress,
per-receiver emitted state, and persistent SNMPv3 engine identity/boot count. No probe ledger or
operational history is kept; use the receiver's event history. Core measurement retention is unchanged.
Spring JDBC (`JdbcClient` and `TransactionTemplate`) manages statements and transactions without
a Spring Boot runtime or application context. The loop exclusively owns one writable connection;
the separate `status`/`health` processes read a consistent snapshot through their own read-only connection.
[StateStore](src/main/java/at/pegelhub/watchdog/state/StateStore.java) owns the watchdog's state transitions,
restart rules, and diagnostic output; its class documentation lists every persisted key with a JSON example.
The package-private [SqliteStateStorage](src/main/java/at/pegelhub/watchdog/state/SqliteStateStorage.java)
documents the SQL schema and owns SQL, JSON document persistence, schema checks,
transactions, and file/connection lifecycle. Callers continue to use only `StateStore`.

One process owns the local state directory via a file lock. Use a dedicated local Docker volume,
not shared/NFS storage. Restart retains emitted errors, so it cannot manufacture an OK; the first
successful fresh read can publish recovery. A persisted clock watermark prevents clock rollback
from making an expired measurement appear fresh again. Both hosts need synchronized clocks.

State schema 2 intentionally rejects the former synthetic watchdog's schema 1 and incompatible
route/receiver configuration. There is no automatic reset or legacy mode. Keep the old volume and
provision a distinct one for this replacement. A genuine route/receiver change also
requires an explicit state cutover and receiver reconciliation, not deletion as a troubleshooting step.
Freshness-threshold tuning, secret rotation, and poll-interval changes reuse the same state after restart.
This build requires the current `return-freshness-v2` identity binding. Older passive bindings
are rejected without resetting their state; automatic identity upgrades are no longer supported.
The next successful read evaluates a changed threshold; configuration edits alone never emit recovery.
Retain a compatible build for rollback rather than replacing the state volume.
Existing schema-2 state with the current identity is reused by the single-loop build: session startup
replaces only the two former worker heartbeats with one `progress` timestamp. Emitted alarms, local
publication failures, the clock watermark, and SNMP engine identity/boot count are retained.

## Running

```sh
mvn -B -ntp -pl tools/e2e-watchdog -am verify
docker build -f tools/e2e-watchdog/Dockerfile -t pegelhub-e2e-watchdog:local .
docker run --name ph-watchdog --stop-timeout 45 --read-only --tmpfs /tmp:exec,size=32m \
  -v return-watchdog-state:/var/lib/pegelhub-watchdog \
  -v /etc/pegelhub/watchdog:/app/config:ro \
  -v /etc/pegelhub/watchdog-secrets:/run/secrets:ro pegelhub-e2e-watchdog:local
docker exec ph-watchdog java -cp '/app/watchdog.jar:/app/lib/*' at.pegelhub.watchdog.WatchdogApplication status
docker exec ph-watchdog java -cp '/app/watchdog.jar:/app/lib/*' at.pegelhub.watchdog.WatchdogApplication health
```

`run [config]` defaults to `/app/config/watchdog.yaml`; `WATCHDOG_STATE_DIR` overrides the state path.
SQLite JDBC needs an executable `/tmp` to load its native library. The root filesystem stays read-only.
The shared Java entrypoint supports the repository's custom CA trust configuration.

`status` is sanitized schema-2 JSON containing the check, loop/publication progress, and public
`snmpEngine.id` (hex) / `boots`. `health` exits nonzero for unavailable/unowned state,
loop inactivity of 180 seconds, or a local SNMP send failure. Such a receiver failure survives
restart and clears only after a later successful emission to that receiver, not on route recovery.
Event-only mode cannot re-test a repaired receiver until the next event is due. Route alarms and Core outages do
not by themselves make the container unhealthy. External host/container health monitoring is required.

## SNMP Contract

Supports one or two receivers, v2c traps for compatibility/local tests, or v3 `authPriv` using
**SHA-256 / AES-256**. No INFORMs or pollable agent. The v3 authoritative engine identity and
incremented boot count are committed before sending; do not clone an active instance's state volume.
Register its public engine ID and credentials with both receivers.

Each packet contains `sysUpTime.0`, `snmpTrapOID.0` set to `trapOid`, and a UTF-8 OctetString at
`messageOid`. The stable, regex-friendly payload has this format (`null` means unavailable evidence):

```text
PH_WATCHDOG version=1 id=dhk-callisto status=ERR reason=stale evaluatedAt=2026-09-16T12:10:01Z observedAt=2026-09-16T12:00:00Z ageSeconds=601.0 emittedAt=2026-09-16T12:10:02Z
```

`read_failed` means "Pruefung nicht moeglich"; `stale` means no sufficiently recent measurement;
`fresh` is recovery. Match the exact `id` and `status=ERR|OK`, not incidental words in the message.
OID examples use the documentation enterprise number and must be replaced with agreed receiver OIDs.

Publication follows the Bauer event-only agreement:

- Healthy startup emits nothing. The first failing check emits ERR, even on startup.
- ERR is followed by OK only after a successful fresh check. Reasons changing within ERR do not resend it.
- No periodic refresh or startup OK. Restart preserves each receiver's last emitted signal.
- A local send failure retries current state for that receiver only, on the first completed read
  cycle at least 30 seconds after the failed attempt. There is no independent retry timer.
  Recovery coalesces an obsolete, never-emitted error; there is no historical event queue.

Traps are recorded as **emitted**, not delivered. UDP traps are unacknowledged
([RFC 3416, section 4.2.6](https://www.rfc-editor.org/rfc/rfc3416.html#section-4.2.6)).
A successful send to an unreachable receiver can still look successful locally. With event-only
publication, a lost ERR or OK is not repaired by a heartbeat; external monitoring and receiver
reconciliation after an outage are operational requirements, not guarantees this tool can provide.
A crash between sending and recording may also produce a duplicate event after restart.

## Acceptance and Rollout

Automated tests cover boundary times, unchanged values, failed reads, restart/clock behavior, SQLite
ownership and rollback, throttled loop heartbeats, shutdown, per-receiver retries, and actual
v2c/v3 UDP decoding. A real-process test exercises Core read failure, ERR/OK publication, and shutdown
with an in-flight read against local HTTP and SNMP fixtures.
Keep tests focused on alarm, health, and recovery behavior rather than retired state formats or
threading details. Shared connector-client behavior is tested in the library, not duplicated here.
The separate `Watchdog Image` workflow validates Compose and runtime health; its optional publish
input selects only this image and never deploys it. Full two-stack fault CI remains deferred.

Use the [manual lab checklist](lab/README.md). Before production agree the exact return series,
exclusive full route, preserved timestamps, emission/poll cadence, maximum age, OIDs/message matching,
receiver addresses/ports, engine ID, secret exchange, and external monitoring responsibilities.

The linked Bauer discussion specifies two receivers and SHA-256/AES-256, but the mentioned
UDP 161 versus 162 still needs explicit confirmation. Test decoding and both ERR/OK interpretations
with the real receivers under separate approval. Local simulations do not establish the production
Callisto route or receiver interoperability. No production changes are included.
