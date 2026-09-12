# Core-Owned Measurement Representations

## Status

Accepted. Extends the representation contract in [ADR 0010](0010-operational-metadata-catalog.md).

## Decision

Core owns conversion between declared representations and canonical values, in
both directions. Connectors select a representation and encode the protocol;
they do not maintain conversion formulas or copies of measuring-point gauge zero.

Canonical storage remains water level in `cm`, temperature in `Cel`, and discharge
in `m3/s`. The source assignment may declare `litres-per-second` for discharge or
`metres-above-adria` for water level, in addition to `canonical`. Source assignment
wire fields and existing values are unchanged. Flyway extends the allowed-source
constraint without rewriting metadata or measurements.

Raw and bucket reads accept an optional `representation` query parameter, default
`canonical`. Responses identify both `representation` and `unit`. Core authorizes
the read before resolving representation metadata and rejects incompatible
properties or missing gauge zero, including for empty windows. Internal latest
reads and monitoring remain canonical.

`MeasurementConversion` is the single owner of conversion arithmetic. It uses an
immutable metadata snapshot for each write batch entry or read. Write processing
checks the actor first, loads each target series and shared hierarchy once, and
checks all target permissions before constructing conversions. The authorization
policy checks permissions against loaded metadata; it does not return or build
converters. Missing gauge zero is a preparation error, not a permission failure.
Absolute levels use the **current** measuring-point gauge zero, not a historical
datum. Bucket
averages can be converted after aggregation because the supported transforms
are affine; sample counts and timestamps do not change.

## Consequences

- `1000 l/s` is stored as `1 m3/s`; an explicit l/s read returns `1000` again.
- At gauge zero `152.68 m above Adria`, `155.56 m above Adria` and `288 cm` are
  alternate representations of the same water level.
- Changing gauge zero affects subsequent conversions, including represented
  reads of older canonical measurements. Datum history is not introduced here.
- IEC mappings replace `gaugeZeroElevationMAboveAdria` with
  `outputRepresentation: metres-above-adria`. Old mapping files are rejected;
  silently reverting to canonical values would be unsafe.
- Explicit represented client reads require Core to confirm representation and
  unit, even for empty responses. Unsupported older clients/Core versions fail
  instead of forwarding wrongly scaled values. Default canonical reads remain
  backward compatible.
- TSTP multi-parameter mapping, display-unit preferences, arbitrary unit systems,
  and changes to QA/Messaging are separate work.

See [configuration and upgrade examples](../guides/measurement-representations.md).
