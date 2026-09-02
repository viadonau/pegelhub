# Use Station, TimeSeries, And Measurement As The Core Data Model

## Status

Partially superseded. The hierarchy remains valid, but the authorization and
vocabulary decisions in this early ADR are superseded by
[ADR 0010](0010-operational-metadata-catalog.md).

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

The rewrite replaced the earlier API and persistence contracts. Connectors now
write measurements by TimeSeries identity, while protocol-specific addresses
and channel names remain in connector configuration.
