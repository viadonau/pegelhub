# Local Acceptance, 2026-09-15

Historical record of the former synthetic watchdog, not the passive return-series replacement.
See [2026-09-16 acceptance](acceptance-2026-09-16.md) for the current implementation.

Executed on the isolated `ph-watchdog-lab` Compose project with real Core A/B, PostgreSQL A/B,
InfluxDB A/B, Keycloak A/B, and the production ICC, TSTP, and IEC connectors. All times below are UTC.
The lab SNMP receiver decoded actual SHA-256/AES-128 authPriv packets. No production system was accessed.

| Scenario | Observed result |
| --- | --- |
| Normal full route | Roundtrip OK at 12:38:04; freshness OK at 12:38:05. Stored PUTs traversed both Cores and returned. |
| Corrupt fresh return | Roundtrip CRITICAL/mismatch at 12:39:04, before deadline. |
| Pause IEC emissions, keep TCP | One IEC connection remained active; freshness CRITICAL at 12:39:08. |
| Restore both fixtures | Freshness OK at 12:41:34; only the new 12:42 probe restored roundtrip OK at 12:42:05. |
| Drop GET returns | First missed probe, observed 12:44:00, triggered CRITICAL/missing at 12:44:31. |
| Delay returns 60 seconds | 12:45 probe became MISSING at 12:45:30. Later API read confirmed 12:45:00/value 766 in A return; alarm stayed CRITICAL. |
| Stop Core A | Freshness UNKNOWN/read_failed at 12:46:34; both checks UNKNOWN by 12:47:15. Runtime health remained true, prior roundtrip alarm remained open. |
| Stop Keycloak A, restart watchdog | Actual startup traps then UNKNOWN/read_failed for both checks at 12:48:02. No clean recovery on failed authentication. |
| Restore authentication | New probe restored roundtrip OK at 12:49:05. |
| Stop/restart IEC connector | Freshness CRITICAL at 12:48:52; fresh receipt restored OK at 12:49:42. |
| Stop Core B only | Roundtrip CRITICAL/missing at 12:50:31; freshness remained OK and container health true. |
| Restore Core B | New 12:51 probe restored roundtrip OK at 12:51:17; 12:50 loss remained historical. |
| Watchdog restart | Received fresh startup notifications with persisted prior alarm/history; no duplicate minute reservation. |

Verification also completed:

- Maven reactor `-Pintegration verify`, including existing Core and connector tests.
- Focused watchdog tests for pure evaluation, UTC rollover, exact matching, duplicate/out-of-order data,
  restart and gap recovery, SQLite locking/corruption, and a blocked submission independent of freshness.
- Real UDP v2c/v3 tests; SQLite closed/reopened between v3 publications, with stable engine ID and incrementing boots.
- Stateful TSTP fixture test with an independent known wire vector for UTC noon/value 721.
- Compose validation and production image checks: non-root, read-only root, writable SQLite volume,
  health during Core unavailability, sanitized JSON, restart, and no simulator classes/dependencies in runtime image.

During verification, fixed a native SQLite load failure on a noexec `/tmp`, an IEC fixture STARTDT/GI
tracking issue, and a health snapshot race where a worker could advance after the CLI sampled its clock.
These fixes affect the watchdog/lab only; production connector behavior was not changed.

This is **local acceptance**, not approval of a deployed Callisto test target. Production range, route,
exclusive ownership, timing, timestamp preservation, receiver settings, and external health monitoring
still require the separately approved acceptance check described in the main README.
