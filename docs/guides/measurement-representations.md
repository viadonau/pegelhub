# Measurement Representations

Core stores one canonical representation per property. Source and output
representations do not change the identity or canonical unit of a time series.

| Property | Stored unit | Additional input/output representation | Returned unit |
| --- | --- | --- | --- |
| `water-level` | `cm` | `metres-above-adria` | `m` |
| `water-temperature` | `Cel` | None | `Cel` |
| `discharge` | `m3/s` | `litres-per-second` | `l/s` |

## Input

For a discharge source that sends l/s, set its Core source assignment to:

```json
{
  "connectorId": "11111111-1111-1111-1111-111111111111",
  "representation": "litres-per-second"
}
```

This is the `sourceAssignment` part of the time-series metadata, not a field on
individual measurement writes. A submitted `1250` is then stored as `1.25 m3/s`.
Do not also divide values in the connector. Other sources remain `canonical`.
Do not change a source's representation while it has buffered values in the old
unit; coordinate the source configuration and pending data first.

For water levels in metres above Adria, use `metres-above-adria` and configure
`gaugeZeroElevationMAboveAdria` on the Core measuring point. Core applies
`cm = (absolute metres - gauge zero) * 100`.

## Output

Existing reads without a representation still return canonical values. Examples
of authenticated GET routes (replace `{id}` with the time-series UUID):

```text
/api/v1/time-series/{id}/measurements?last=24h&representation=litres-per-second
/api/v1/time-series/{id}/measurements?last=365d&order=desc&limit=1&representation=metres-above-adria
/api/v1/time-series/{id}/measurements/buckets?last=24h&bucket=5m&representation=litres-per-second
```

Both response envelopes include `representation` and `unit`. Absolute water
levels have unit `m` and representation `metres-above-adria`, so the datum is
explicit. Missing gauge zero, unknown representations, and property mismatches
fail with HTTP 400, even when the window contains no measurements. Existing
authentication and read grants still apply.

The shared Java client's range/latest overloads accept `MeasurementRepresentation`
and verify Core's response metadata. They return values already converted by
Core; callers must not convert them again.

## IEC Upgrade

1. Deploy the new Core before enabling represented connector reads. Its Flyway
   migration only extends the catalog constraint; it does not rewrite history.
2. Check that Core's measuring point has the correct gauge zero. Compare it with
   any existing connector-local value before switching ownership.
3. Replace the old `gaugeZeroElevationMAboveAdria` field in outbound mapping files
   with `outputRepresentation: metres-above-adria`. Omitted output representation
   means `canonical`. A discharge output can select `litres-per-second`.
4. Review an authenticated represented Core read before activating the connector.
   An unsupported Core or unresolvable datum fails the mapping's read; there is
   no fallback to sending canonical values under the wrong unit.

```yaml
iecIoa: 123
timeSeriesId: "22222222-2222-2222-2222-222222222222"
direction: core-to-external
outputRepresentation: metres-above-adria
```

Legacy local-gauge-zero mapping fields are rejected at startup, even when an
output representation is also present. Keep the original files for a deliberate
rollback; old IEC binaries do not understand `outputRepresentation`. Likewise,
an old Core does not understand l/s source assignments. Do not roll back binaries
without coordinating those configuration changes. These examples do not modify
any deployed configuration automatically.
