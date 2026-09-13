# ADR 0012: Integrate Quality and Notifications into Core

## Status

Accepted, 2026-09-13. Supersedes the QA/Messaging portion of ADR 0007.

## Decision

Use a modular monolith for one Core instance. Keep `quality` and `notifications`
as feature packages, each owning its persistence, scheduled tasks and public
application interface. Quality calls `InternalMeasurements` and `Notifications`
in process. The data hub must not depend on either feature, and feature code
must not access another feature's repositories. Focused dependency tests enforce
these boundaries without introducing a plugin framework.

The Angular frontend remains independently deployed. Protocol connectors remain
independent technical adapters. An internal producer is not a Connector and does
not receive synthetic credentials or connector permissions.

## Rationale

One maintainer and one product boundary do not justify separate authentication,
deployment and failure handling for every feature. Possible future student-team
contributions can use the application interfaces and feature packages; they do
not currently require independently installable services. A separate worker would
mean a separate process/deployment, not a separate developer.

## Consistency and Failure Boundaries

- Named profiles snapshot configuration for every run. QA is advisory; ingestion
  and normal reads do not depend on QA completion or transport availability.
- A single scheduler executes recent-window QA with bounded reads. A separate
  scheduler sends durable PostgreSQL deliveries using short claims and leases.
- Findings and notification enqueue commit in one PostgreSQL transaction.
  Influx reads/writes and transport calls occur outside that transaction.
- Derived measurements use stable producer/time-series/observation identity.
  A failed output write is reported separately and may have been accepted.
- Delivery acceptance is not proof of recipient receipt. Ambiguous network
  failures can lead to duplicates; retries do not create new delivery records.
- Jobs default off. There is no historical backfill, incident lifecycle, broker,
  plugin runtime or multi-instance execution coordination in this decision.

## Cutover and Rollback

Remove standalone apps, images, Compose services and obsolete OAuth clients.
Configuration imports use only the new non-secret formats. Apply additive Flyway
migrations; do not reset databases or rewrite the initial migration.

After internal-producer measurements exist, rollback must retain origin-aware
Core measurement queries. Disable the two jobs first; do not deploy an older,
connector-only reader or undo provenance columns. See the operational runbook.
