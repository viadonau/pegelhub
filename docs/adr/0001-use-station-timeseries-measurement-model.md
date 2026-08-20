# Superseded: Use Station, TimeSeries, And Measurement As The Core Data Model

The hierarchy remains valid, but the authorization and vocabulary decisions in
this early ADR are superseded by [ADR 0010](0010-operational-metadata-catalog.md).

PegelHub replaces the overloaded Supplier/Taker measurement model with
Connector, StationOwner, Station, MeasuringPoint, TimeSeries, and Measurement.
The operational catalog and its explicit read-access relations are defined in
ADR 0010.

## Considered Options

- Keep Supplier and Taker as core concepts and gradually clean up their fields.
- Rename Supplier to Station and keep measurements grouped by the old Supplier identifier.
- Split the model now around Connector, StationOwner, Station, MeasuringPoint,
  TimeSeries, and Measurement.

## Consequences

Existing API contracts and persistence tables may break during the rewrite. Connectors should eventually write measurements by TimeSeries identity, while compatibility mappings from protocol-specific addresses or channel names can live outside the core Measurement model.
