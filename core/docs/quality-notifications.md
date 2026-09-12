# Quality and Notifications Operations

Quality and notifications are Core feature packages. The main Angular frontend
provides German operator screens. Connectors remain separate. Run only one Core
instance with these jobs enabled; there is no distributed QA execution protocol.

For maintainers, application Javadoc describes ownership, snapshots and failure
boundaries. The [code comprehension guidance](../../docs/architecture/code-comprehension-guidance.md)
records the research and its limits behind these documentation and extraction choices.

## Configuration and Enablement

Both jobs default off: `PEGELHUB_QUALITY_ENABLED=false` and
`PEGELHUB_NOTIFICATIONS_ENABLED=false`. Configure destinations and profiles, then
enable the required jobs in the local/staging environment and restart Core.
Profile/destination `enabled` flags are separate from global execution flags.
Disabling a destination cancels pending deliveries, but cannot recall an already
in-flight network call. Accepted in-flight deliveries remain recorded as accepted.

Spring properties:

| Property | Default |
| --- | --- |
| `pegelhub.quality.maximum-points` | 100000 per run |
| `pegelhub.quality.timeout-seconds` | 120 |
| `pegelhub.quality.retention-days` | 90 |
| `pegelhub.notifications.retention-days` | 90 |

Profiles default to a 1200-second recent window and 60-second fixed delay after
completion. Maximum window is seven days. A single QA execution slot prevents
overlap, and restart/recovery does not backfill missed schedules. Database
availability is still required to record outcomes; interrupted lifecycle writes
are recovered on the next scheduler tick after connectivity returns.

## Deployment-Managed Credentials

Configuration APIs and YAML exports contain credential *references*, never
passwords/community values. Supply credentials through protected Spring
configuration. The optional `deploy/single-host/operational.compose.yaml` overlay
works with local or staging Compose. Set `PEGELHUB_OPERATIONAL_CONFIG_FILE` to an
absolute, deployment-managed file path and ensure the container's Core user can
read it. Do not commit that file. Set the path in the protected environment file;
the canonical local-stack and staging deploy/rollback helpers include the overlay
automatically. Include it explicitly when invoking Docker Compose directly.

Example shape (replace deployment values outside the repository):

```yaml
pegelhub:
  notifications:
    credentials:
      mail-relay:
        kind: SMTP
        host: smtp.example.internal
        port: 587
        start-tls: true
        ssl: false
        username: ${PH_SMTP_USERNAME}
        password: ${PH_SMTP_PASSWORD}
      snmp-operations:
        kind: SNMP_V3
        username: ph-notifier
        security-level: authPriv
        auth-protocol: SHA256
        auth-password: ${PH_SNMP_AUTH_PASSWORD}
        priv-protocol: AES
        priv-password: ${PH_SNMP_PRIV_PASSWORD}
```

Environment placeholders must be passed into the Core container by the operator's
Compose override, or resolved by deployment tooling into the protected file.
For SNMPv2c use `kind: SNMP_V2C` and deployment-managed `community`.
SNMPv3 supports `noAuthNoPriv`, `authNoPriv`, `authPriv`; SHA/SHA256 and AES (128-bit)/AES256
are supported. MD5/DES require `allow-legacy-algorithms: true`. Engine identity
and boot counter are persisted in PostgreSQL; preserve that database across restarts.
Do not run multiple Core processes sharing that engine identity.

SMTP uses bounded connection/read/write timeouts and required STARTTLS when
enabled; SSL verifies the server identity. Optional missing credentials or
unreachable transports do not participate in readiness or startup checks.
Invalid delivery configuration terminates visibly instead of retrying forever.

## Permissions and Existing Keycloak Realms

User `system:admin` is required for profiles, destinations, tests and delivery
history. Findings require existing monitoring read permissions (or user admin).
Service submissions require CLIENT actor plus `messaging:send`. Existing issuer,
audience and actor checks remain in force.

New local/staging seeds allow the main frontend scope to carry an already-assigned
administrator role. Monitoring users are **not** granted that role. For an
existing realm, authenticate `kcadm.sh` with an administrator in the intended
environment, then run the additive helper:

```bash
KCADM=/path/to/kcadm.sh REALM=pegelhub \
  deploy/single-host/scripts/migrate-operational-scopes.sh
```

The helper adds only the frontend scope mapping. It does not replace realms,
users, groups, passwords or other client configuration. After stopping standalone
services, run it with `--retire-standalone-clients` to remove only
`pegelhub-modules-frontend` and `qa-module` (including its service account).
Do not rerun realm import to migrate existing installations. Log out/in to obtain
the changed scope. The helper uses the standard Keycloak
[client scope-mapping API](https://www.keycloak.org/docs-api/latest/rest-api/index.html#_scope_mappings).

## API and Import/Export

- `/api/v1/quality/profiles`: list/create; `/{id}` read/update;
  `/{id}/run` accepts a run-now request, returning 202.
- `/api/v1/quality/runs`: paginated history; `/{id}/findings` paginated evidence.
- `/api/v1/notifications`: POST configured destination IDs, subject and body;
  returns 202 with `requestId`. No arbitrary host or secret submission is accepted.
- `/api/v1/notifications/destinations`: list/create; `/{id}` read/update.
- `/api/v1/notifications/deliveries`: paginated history, including retry outcomes.
- Profiles and destinations provide `/{id}/export` and `/import` YAML endpoints.
  Import creates a new named configuration using the same domain validation as
  normal edits. Unknown fields, secrets, invalid references and oversized
  documents are rejected. No legacy adapters exist.

Use exported files as format examples. UUID references identify existing catalog
series and destinations, so importing between environments requires valid local
references. Disable rather than delete configurations with operational history.

## Outcomes, Retention and Recovery

QA never gates ingestion or ordinary reads. Findings and queued messages commit
atomically in PostgreSQL. Influx operations do not share that transaction.
Output failure is recorded separately; an ambiguous failure may already have
written a point. Stable producer/series/observation identity makes repeat writes
updates rather than additional points. Old connector tags remain unchanged.

Notifications use a separate sender and recoverable 300-second claims. Five total
attempts use delays of 30, 60, 120 and 240 seconds. Network calls occur outside
transactions. Acceptance is not proof of receipt, and ambiguous failures can
produce duplicates. Every QA run with findings notifies again; retries reuse the
same delivery and routing snapshot. Editing a route never reroutes queued work.
An in-flight send cannot be recalled. SMTP timeouts bound individual socket
operations, not the complete multi-recipient attempt; a slow relay can delay
later notifications, but runs on neither the QA nor ingestion execution path.

Completed deliveries are purged before associated QA history. Pending references
retain their runs. Measurement bucket retention is unchanged. Inspect sanitized
history and correlation IDs in logs; never log deployment secrets to troubleshoot.

## Cutover and Immediate Fallback

Back up PostgreSQL using the normal operational procedure, stop standalone QA and
Messaging, deploy the origin-aware Core and main frontend, migrate Keycloak scope,
recreate configuration in the new formats, then explicitly enable jobs. Flyway
V3-V5 add the operational modules after the measurement-representation migration
V2 from PR #62. Existing connector assignments, including litres per second,
remain unchanged. QA reads and derived writes always use canonical storage units;
external reads may request another representation through Core's conversion API.
No V1/V2 rewrite or database reset is needed.

The earlier, unmerged QA preview used V2-V4 for these tables. Do not apply this
rebased migration chain to a database with that preview history or repair its
checksums blindly; reconcile that history explicitly before upgrading. This
cutover assumes the canonical V1/V2 history, not a previously migrated QA preview.

Immediate fallback: disable both global job flags. After any internal-producer
measurement exists, retain an origin-aware Core build even during rollback.
Do not roll back to connector-only Influx queries, remove producer metadata, or
reset either database. There is no cross-database rollback or output retraction.

## Verification

`mvn -B -ntp -Pintegration verify` covers Core/connector contracts, mixed Influx
origins, ownership conflicts, QA rules, transaction rollback, recovery, retention,
SMTP on a local test server and SNMP modes on UDP receivers. Frontend checks,
Chromium tests and production image validation remain in the main frontend CI.
The scope migration has an additive/idempotent mock-CLI test; staging bootstrap
tests keep monitoring-user grants unchanged.
