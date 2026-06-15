# Use Flexible TimeSeries Property And Unit Codes

TimeSeries will start with string codes for observed property and unit instead of Java enums or dedicated catalog tables. This keeps the rewrite focused on the core Station/TimeSeries/Measurement split while still allowing operators to model water level, discharge, water temperature, air temperature, and later property types without a code or schema migration for every new observed value.

## Considered Options

- Use Java enums for observed property and unit.
- Add normalized catalog tables for observed properties and units in the first rewrite pass.
- Store observed property and unit as validated non-blank codes on TimeSeries.

## Consequences

The first rewrite pass gets less compile-time control over allowed properties and units. A catalog or stricter validation can be added later if operators need controlled vocabularies, translations, or unit conversion.
