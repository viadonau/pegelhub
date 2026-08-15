# Introduce MeasuringPoint Between Station And TimeSeries

PegelHub will introduce `MeasuringPoint` as the stable physical observation
position between `Station` and `TimeSeries`.

## Considered Options

- Keep `TimeSeries` directly under `Station` and duplicate physical-location
  metadata on every series.
- Treat each installed sensor or device as the parent of its `TimeSeries`.
- Introduce a device-independent `MeasuringPoint` that groups the series
  observed at one physical position.

## Decision

A `Station` contains one or more `MeasuringPoint` records. A `TimeSeries`
belongs to exactly one `MeasuringPoint` and no longer owns a direct Station
relationship.

`MeasuringPoint` owns the metadata shared by series at the same physical
position: name, bank, river kilometer, reference level and year, and the RNW,
MW, HSW, and HW100 reference values. `TimeSeries` continues to own the observed
property, unit, external mapping code, and source Connector.

A MeasuringPoint is deliberately not a sensor or device. Installed equipment
can be replaced while the physical observation position and its TimeSeries
remain stable. Device inventory and lifecycle are outside this slice.

The metadata API exposes MeasuringPoints directly and supports listing them by
Station. TimeSeries creation and filtering use `measuringPointId`. Station-wide
authorization continues to cover descendant TimeSeries by resolving the
TimeSeries through its MeasuringPoint. MeasuringPoint is not added as a direct
AccessGrant resource in this slice.

Existing TimeSeries rows are migrated without losing metadata. Rows at the same
Station with an identical location/reference metadata tuple are assigned to one
backfilled MeasuringPoint. Different tuples remain separate because the system
cannot safely infer that they describe the same physical position.

## Consequences

The operator frontend uses TimeSeries as the monitoring row and route identity.
Each row still presents its MeasuringPoint metadata, and the detail view
resolves the TimeSeries through its MeasuringPoint. It no longer promotes an
arbitrary observed property as the identity of a row, nor does it make a
MeasuringPoint the route target.

Creating a TimeSeries now requires an existing MeasuringPoint. Clients that
create metadata must create or select the point first. Connector measurement
writes remain unchanged because they continue to address a TimeSeries by ID.

Moving a TimeSeries between points, merging migrated points, and managing the
installed sensor/device lifecycle remain separate future operations.
