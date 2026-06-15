# Use Station, TimeSeries, And Measurement As The Core Data Model

PegelHub will replace the overloaded Supplier/Taker measurement model with Connector, StationOwner, Station, TimeSeries, AccessGrant, and Measurement. The current Supplier concept mixes station metadata, data-provider identity, authorization, and Influx grouping; the new model keeps technical identity on Connector, resource authorization in AccessGrant, stable hydrological metadata in Station and TimeSeries, and time-series values in Measurement.

## Considered Options

- Keep Supplier and Taker as core concepts and gradually clean up their fields.
- Rename Supplier to Station and keep measurements grouped by the old Supplier identifier.
- Split the model now around Connector, StationOwner, Station, TimeSeries, AccessGrant, and Measurement.

## Consequences

Existing API contracts and persistence tables may break during the rewrite. Connectors should eventually write measurements by TimeSeries identity, while compatibility mappings from protocol-specific addresses or channel names can live outside the core Measurement model.
