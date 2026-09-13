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

`MeasurementConversion` provides both conversion directions and the output unit.
`CanonicalConversion` leaves values unchanged, `LitresPerSecondConversion` handles
discharge scaling, and `MetresAboveAdriaConversion` handles absolute water levels.
Only the last of these holds a gauge zero. The measurement service selects the
converter and checks that the property supports the requested representation.
It creates one converter per target series in a write batch, or per raw or bucket
read. Each converter keeps its settings for that operation; only absolute reads
need to load the measuring point.

For writes, Core first checks that the caller is allowed to write as a connector.
It then loads each series, measuring point and station once and checks access to
every target before creating the converters. A missing gauge zero should not
cause a conversion error to be returned instead of a permission error for another
target. The authorization policy checks access using the loaded metadata; it does
not create converters.

Loading metadata once lets access checks and conversions use the same records,
but the database can still change while those records are being loaded. Core only
passes the batch to the repository after every value has been converted. If the
database write then fails, Core does not roll back any values already stored.

Absolute water levels use the measuring point's **current** gauge zero, not its
value at observation time. The supported conversions only scale or shift values
(affine conversions), so converting an average gives the same result as converting
each sample before averaging. Sample counts and time ranges stay the same.

## Consequences

- `1000 l/s` is stored as `1 m3/s`; an explicit l/s read returns `1000` again.
- At gauge zero `152.68 m above Adria`, `155.56 m above Adria` and `288 cm` are
  alternate representations of the same water level.
- Changing gauge zero affects subsequent conversions, including represented
  reads of older canonical measurements. Datum history is not introduced here.
- IEC mappings replace `gaugeZeroElevationMAboveAdria` with
  `outputRepresentation: metres-above-adria`. Old mapping files are rejected;
  silently reverting to canonical values would be unsafe.
- All client reads require Core to confirm representation and unit, even for empty
  responses. Client implementations must support representation-aware reads;
  shorter overloads simply request canonical values. Core and connectors must be
  upgraded together: responses without this metadata are rejected, not treated as
  canonical values.
- TSTP multi-parameter mapping, display-unit preferences, arbitrary unit systems,
  and changes to QA/Messaging are separate work.

See [configuration and upgrade examples](../guides/measurement-representations.md).
