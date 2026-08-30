# Split Technical Telemetry From Measurements

## Status

Accepted and partially implemented. Measurements and telemetry use separate
APIs and InfluxDB buckets. The existing telemetry payload still contains legacy
water- and air-temperature fields pending a dedicated telemetry redesign.

PegelHub keeps technical connector telemetry separate from hydrological
Measurements. New values that describe the station or water body, such as water
temperature, belong in TimeSeries Measurements. Values that describe connector
runtime or health, such as battery voltage, cycle time, IP addresses, or field
strength, belong in connector telemetry.

## Considered Options

- Keep the existing Telemetry model unchanged.
- Fold all telemetry fields into TimeSeries Measurements.
- Split station-observed values into TimeSeries Measurements and connector runtime values into a separate telemetry model.

## Consequences

The Measurement model is independent of the legacy-shaped Telemetry record.
Existing telemetry temperature fields remain a compatibility concern until that
slice is redesigned; they do not establish the model for new environmental
observations.
