# Manual Two-Core Return-Series Lab

Run from the PH repository root with Docker Compose. The isolated project
`ph-return-watchdog-lab` uses its own network, volumes, and loopback-only ports.
The former `ph-watchdog-lab` volumes are left untouched. Each Core has separate PostgreSQL,
InfluxDB, and real Keycloak authentication. No frontend or QA module is needed.

```text
IEC fixture -> IEC connector -> A outgoing -> ICC -> B outgoing -> TSTP PUT
                                                                    |
                                                             recorded PUT data
                                                                    |
                  watchdog <- A return   <- ICC <- B return   <- TSTP GET
                     |
              two SNMPv3 receivers
```

The watchdog has only read access to A return, no write role or source assignment.
Each leg has its own exclusive connector writer. IEC emits a constant level with advancing timestamps,
so the entire real connector path supplies freshness without a watchdog-generated test signal.

Protocol fixtures were adapted from `pegelhub-connector-live-suite`; no sibling checkout is needed.
TSTP stores actual PUTs and returns those measurements, not canned successes. Replayed writes keep
their first receipt time so delay injection remains stable. Fixture storage is ephemeral; restarting
it clears its data and production connector overlap may replay measurements. Test fixtures are absent
from the production watchdog image.

## Commands

```sh
sh tools/e2e-watchdog/lab/lab.sh start
sh tools/e2e-watchdog/lab/lab.sh inspect
sh tools/e2e-watchdog/lab/lab.sh traps
sh tools/e2e-watchdog/lab/lab.sh stop
# Explicit destructive reset of this lab's volumes only:
sh tools/e2e-watchdog/lab/lab.sh reset DELETE-LAB-DATA
```

Core A/B: `localhost:18080` / `localhost:28080`; Keycloak A/B: `localhost:18082` / `localhost:28082`;
fixture status: `http://localhost:18030/status`. These ports must be free before starting.
Internal issuer names are Docker hostnames. Disposable realm operator: `operator` / `lab-password`,
client `lab-operator`; master admin: `admin` / `lab-password`. Never reuse lab credentials in production.

Bootstrap uses supported APIs, stores generated metadata IDs in the config volume, and preserves them
on ordinary restart. Do not independently reset one Core's storage. Connector accounts have measurement
roles; the watchdog has only `measurement:read` and an exact grant for A return.

Connectors poll every two seconds; the watchdog polls every second, with a ten-second age limit.
Allow up to a minute for initialization. `traps` and `secondTraps` in fixture status are **received and
decoded** SNMPv3 SHA-256/AES-256 messages on ports 1162/1163, not sender emission markers.

### Inspect Received Traps

Open [the decoded packet view](http://localhost:18030/traps), or run `lab.sh traps`.
Each receiver shows its latest 100 packets with receipt time, sender address, SNMP version,
security level, PDU type, and every variable binding's OID, type, and value. The watchdog message
is also shown as `payload`. Transport credentials and the v2c community are omitted.
This is a decoded view, not a raw packet capture; history is in memory and clears when fixtures restart.
The receiver ports are internal to the lab network; only the HTTP inspection view is published on localhost.

To watch an error and its recovery, pause local emissions with `lab.sh fault 'iec=paused'`, wait
roughly 15 seconds, then reload the packet view. Resume with `lab.sh fault 'iec=running'` and reload
after fresh data traverses the connectors. Both receivers should get one `ERR/stale`, then one `OK/fresh`.
Healthy operation alone does not generate new traps. These controls affect only the isolated lab.

## Fault Checklist

1. **Normal:** check becomes OK; TSTP puts/points and the return timestamp advance. A healthy startup
   does not emit an OK. An initially empty series may produce ERR then OK during route initialization.
2. **Connected but silent IEC:** `lab.sh fault 'iec=paused'`. Connection remains open; the return timestamp
   stops advancing, then `CRITICAL/stale` and one ERR arrive at both receivers.
   `lab.sh fault 'iec=running'` restores fresh data and one OK.
3. **Dropped return:** `lab.sh fault 'tstp=drop'`. TSTP PUT still receives data, but the return stops
   advancing and alerts. `lab.sh fault 'tstp=normal'` restores OK on a sufficiently fresh return.
4. **Delayed return:** `lab.sh fault 'tstp=delay&delaySeconds=30'`. Continuing returns older than
   ten seconds stay ERR. Restore normal mode for recovery; a late old return alone cannot clear it.
5. **Fresh but wrong return:** `lab.sh fault 'tstp=corrupt'` adds one to values but preserves timestamps.
   The check deliberately remains OK. This demonstrates the boundary: no value/integrity validation.
   Restore normal afterwards.
6. **Core A outage:** stop `core-a`. Check becomes `UNKNOWN/read_failed` and sends ERR, not OK.
   Local health stays healthy while the loop progresses. Start Core and require a fresh successful read.
7. **Authentication outage:** stop `keycloak-a`, then restart only `watchdog` to discard its cached
   access token. Check becomes `UNKNOWN/read_failed` and sends ERR. Start Keycloak for fresh-read recovery.
   Check that status and watchdog logs contain no secrets.
8. **Connector/Core B outage:** stop/start `icc`, `tstp`, `iec`, or `core-b` individually.
   An unavailable return route must eventually age into ERR and recover on a fresh timestamp.
9. **Watchdog restart while ERR:** pause IEC and wait for ERR, then restart watchdog.
   No startup OK or repeat ERR; old data remains stale. Resume IEC and verify one OK at both receivers.
10. **No heartbeat:** leave the check healthy or failed for more than five minutes.
    Neither receiver should gain a periodic status trap. External monitoring must detect watchdog loss.

```sh
docker compose -p ph-return-watchdog-lab -f tools/e2e-watchdog/lab/compose.yaml stop core-a
docker compose -p ph-return-watchdog-lab -f tools/e2e-watchdog/lab/compose.yaml start core-a
docker compose -p ph-return-watchdog-lab -f tools/e2e-watchdog/lab/compose.yaml restart watchdog
docker compose -p ph-return-watchdog-lab -f tools/e2e-watchdog/lab/compose.yaml logs --tail 30 icc tstp iec watchdog
```

Keep an acceptance record with UTC times, check transitions, and actual traps at both receivers.
See [2026-09-16 acceptance](acceptance-2026-09-16.md) for this implementation's local test results.
The [2026-09-15 record](acceptance-2026-09-15.md) describes the former synthetic implementation,
not this passive replacement. Local testing authorizes no production changes.
