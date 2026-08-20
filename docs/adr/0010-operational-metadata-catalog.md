# Operational Metadata Catalog

## Status

Accepted. This decision supersedes ADR 0003, clarifies ADR 0005, and replaces
the vocabulary policy in ADR 0009.

## Context

The operator workflow needs a small, dependable catalog rather than arbitrary
metadata strings, contact aggregates, or polymorphic access grants. Development
and staging may reset their metadata, so a clean baseline is preferable to a
compatibility migration chain.

## Decision

Keep the hierarchy `StationOwner -> Station -> MeasuringPoint -> TimeSeries`.
Use exact `BigDecimal` metadata values and shared `active`/`inactive` statuses.
Remove Contact, station numbers, free-text station locations, TimeSeries unit
storage, external codes, and WRITE access grants.

Observed properties are the closed catalog `water-level` (`cm`),
`water-temperature` (`Cel`), and `discharge` (`m3/s`). A TimeSeries is unique
by measuring point and property; both are immutable. Units are derived. A
nullable source assignment stores the Connector and a compatible representation
(`canonical`, or `metres-above-adria` for water level). Core normalizes the
latter with the current PNP at ingestion.

Connector read access is represented by explicit foreign-key-backed station and
time-series tables. A source assignment is the sole resource-level write
authorization. Coarse authorities remain in HTTP security, while actor type,
active hierarchy, connector state, and resource access are checked in
application policies.

Expose a read-only observed-property catalog and keep the monitoring read model
as a small same-store CQRS module. Monitoring collection rows are effectively
active TimeSeries; detail remains available for inactive series. Chart buckets
remain an independent measurement endpoint.

## Consequences

The API and database contract are smaller and deterministic. Connectors must
map protocol-specific identifiers locally and send canonical measurement
values, except for the explicitly supported absolute water-level input. New
properties require a deliberate catalog change. Administration screens,
generic query frameworks, datum history, and compatibility routes remain out of
scope.
