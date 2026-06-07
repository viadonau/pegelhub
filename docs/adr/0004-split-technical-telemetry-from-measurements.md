# Split Technical Telemetry From Measurements

PegelHub will keep technical connector telemetry separate from hydrological Measurements. Values that describe the station or water body, such as water temperature or air temperature, should be modeled as TimeSeries Measurements; values that describe connector runtime or health, such as battery voltage, cycle time, IP addresses, software version, or field strength, belong to a connector telemetry/runtime model.

## Considered Options

- Keep the existing Telemetry model unchanged.
- Fold all telemetry fields into TimeSeries Measurements.
- Split station-observed values into TimeSeries Measurements and connector runtime values into a separate telemetry model.

## Consequences

The first rewrite should not let the existing Telemetry class dictate the Measurement model. Telemetry cleanup can happen after the main Measurement rewrite, but the target direction is clear: observed environmental values are Measurements; technical runtime values are Connector telemetry.
