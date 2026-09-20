# Local Acceptance, 2026-09-16

This record covers the passive return-series replacement of the synthetic watchdog.
Executed only in isolated `ph-return-watchdog-lab`: two real Core instances, separate PostgreSQL,
InfluxDB and Keycloak, production IEC/ICC/TSTP connectors, and two local SNMPv3 SHA-256/AES-256 receivers.
No production access, changes, or deployment. All times are UTC.

## Observed Behavior

The constant IEC level travels through A outgoing -> ICC -> B outgoing -> recorded TSTP PUT/GET ->
B return -> ICC -> A return. Watchdog polls A return once per second with a ten-second age limit.

The trap columns are cumulative **received and decoded** packet counts at both receivers.
They are not merely sender emission markers. Initial empty-series ERR was received at 11:47:09,
followed by fresh-data OK at 11:47:16.

| Scenario | Observation time | Check | Received traps A / B |
| --- | --- | --- | --- |
| IEC paused | 2026-09-16T11:49:30Z | CRITICAL/stale | 3 / 3 |
| Restart during IEC outage | 2026-09-16T11:49:47Z | CRITICAL/stale | 3 / 3 |
| IEC recovery | 2026-09-16T11:50:11Z | OK/fresh | 4 / 4 |
| Corrupt fresh returns | 2026-09-16T11:50:49Z | OK/fresh | 4 / 4 |
| Dropped TSTP returns | 2026-09-16T11:51:18Z | CRITICAL/stale | 5 / 5 |
| Delayed but continuing returns | 2026-09-16T11:51:51Z | CRITICAL/stale | 5 / 5 |
| TSTP recovery | 2026-09-16T11:52:09Z | OK/fresh | 6 / 6 |
| Core A outage | 2026-09-16T11:53:58Z | UNKNOWN/read_failed | 7 / 7 |
| Core A recovery | 2026-09-16T11:54:55Z | OK/fresh | 8 / 8 |
| Authentication outage after watchdog restart | 2026-09-16T11:55:26Z | UNKNOWN/read_failed | 9 / 9 |
| Authentication recovery | 2026-09-16T11:56:03Z | OK/fresh | 10 / 10 |
| Core B outage | 2026-09-16T11:56:36Z | CRITICAL/stale | 11 / 11 |
| Core B recovery | 2026-09-16T11:57:22Z | OK/fresh | 12 / 12 |
| ICC stopped | 2026-09-16T11:57:57Z | CRITICAL/stale | 13 / 13 |
| ICC recovery | 2026-09-16T11:58:22Z | OK/fresh | 14 / 14 |
| TSTP connector stopped; start quiet interval | 2026-09-16T11:58:53Z | CRITICAL/stale | 15 / 15 |
| No periodic traps after more than five minutes | 2026-09-16T12:04:25Z | CRITICAL/stale | 15 / 15 |
| TSTP connector recovery | 2026-09-16T12:04:48Z | OK/fresh | 16 / 16 |
| IEC connector stopped | 2026-09-16T12:05:17Z | CRITICAL/stale | 17 / 17 |
| IEC connector recovery; final healthy state | 2026-09-16T12:05:59Z | OK/fresh | 18 / 18 |

During IEC pause, the simulator still reported one connected IEC client. Restart retained the same
public engine identity, incremented boots from 1 to 2, and did not add a duplicate ERR or premature OK.
Later restarts, including the final publisher-health fix, retained that identity through boot 4.

With the TSTP connector stopped, the failed check ran for another 331 seconds without any periodic
trap: both receivers stayed at 15 packets. Restarting TSTP produced exactly one OK each. The final
IEC stop/recovery test ended with healthy workers, a fresh check, and 18 identical packets per receiver.

The corrupt-return test read actual API values at 11:50:50: outgoing 281 cm, returned 282 cm with a
recent timestamp. The check remained OK and neither receiver gained an event. This is the intended
limit of passive freshness monitoring, not proof of value integrity.

Delayed TSTP data continued advancing its timestamp, but was approximately 35 seconds old and remained
CRITICAL. Core A and authentication outages became UNKNOWN/read_failed and emitted ERR; local health
remained healthy while workers could progress. Successful fresh reads produced OK at both receivers.

The real runtime token carried only `measurement:read`. Return-series reads succeeded;
outgoing-series access was denied with HTTP 403. No write role, admin role, or source ownership
was assigned to the watchdog.

## Automated Verification

- `mvn -B -ntp verify -Pintegration`: all 426 tests passed, including 20 Core integration tests,
  72 shared-client tests and 15 watchdog/fixture tests; no failures, errors, or skips.
- Focused `-pl tools/e2e-watchdog -am clean verify` passed before the final publisher-health
  regression was added; full reactor verification above includes that final fix.
- Real UDP tests decoded v2c and SHA-256/AES-256 v3, checked two receivers, no startup healthy OK,
  no periodic refresh, failed reads, local receiver failures, and persisted engine/boot state.
- SQLite tests checked ownership, incompatible/legacy/corrupt storage, backward-clock recovery,
  independent worker progress, and local emission failures surviving restart.
- Compose configuration validated. Image build and `tests/image-check.sh` passed on the final
  runtime image: non-root UID 10001, read-only root, SQLite restart, healthy with unreachable Core,
  no secret in status, no simulator/test artifacts in the production runtime.

## Review

### Standards

No documented-standard or actionable maintainability findings. No extra abstraction recommended.
Residual test gap: slow DNS delaying the second receiver was not exercised; immediate resolution
failure was tested. This does not affect the separate freshness worker.

### Spec

One local-health defect was found and fixed: coalescing an unsent alarm after route recovery had
cleared a receiver's local send failure. Failures now persist per receiver across restart until
a successful emission to that receiver. Regression tests passed and follow-up review found no
remaining issue. Current-state coalescing itself is deliberate; there is no historical event queue.

Summary: Standards 0 findings; Spec 1 finding fixed, 0 remaining.

## Production Limits

Local simulation does not prove the real Callisto route or interoperability with the production
SNMP receiver. The exact return series, complete route, expected emission cadence, age limit, both
receiver ports (161 versus 162), OIDs/regex interpretation, credentials/engine registration and
external watchdog-health monitoring still require agreement and separately approved acceptance.

Event-only UDP cannot guarantee delivery or repair a lost ERR/OK with a periodic refresh.
Preserve/reconcile receiver alarm state when changing a route or replacing a state volume.
The old synthetic lab's volumes are untouched.
The return-series lab was stopped after verification; its named volumes were retained as well.
