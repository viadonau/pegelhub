# Add A Monitoring Read Model

## Status

Accepted and implemented.

## Context

The operator overview and detail screens need complete rows that combine
TimeSeries metadata, MeasuringPoint and Station relationships, StationOwner
labels, and the newest measurement in a bounded time window. Making the
frontend fan out over generic metadata endpoints creates partial loading and
cross-resource orchestration that belongs to the workflow boundary.

## Decision

Add a small `at.pegelhub.monitoring` application and HTTP module with one
collection read and one detail read. The module composes the existing
application services and uses one grouped InfluxDB latest-measurement query.
It resolves one window per request with the injected clock and returns a
complete response or an error. The collection is deliberately unpaginated
while the operator catalog is bounded.

This is a practical same-store CQRS read model. It does not introduce a query
bus, event sourcing, a materialized database, caching, or a generic expansion
framework. The existing administrative metadata endpoints and independently
reloadable measurement bucket endpoint remain unchanged.

## Consequences

The frontend owns display formatting only and makes one monitoring request per
overview or detail workflow. Influx outages fail the monitoring response as a
whole. If the catalog outgrows the bounded assumption, pagination or a backend
projection is a separate scale-driven change.
