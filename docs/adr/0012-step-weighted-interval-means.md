# Step-Weighted Interval Means

## Status

Accepted for local feature development; production activation is out of scope.

## Decision

`GET /api/v1/time-series/{id}/measurements/intervals` replaces the former
chart-resolution bucket endpoint; raw measurements remain available. Requests use a half-open range
`[from, to)` with aligned boundaries for `15m`, `1h`, or `1d`. Unaligned
requests fail explicitly. `closedOnly` defaults to true and omits windows
ending after the request's `computedAt` instant; the response retains the
requested range so an omitted open suffix remains visible.

Core owns interval calculation. Every TimeSeries exposes the same
`time-weighted-step` interval view. Connector type and observed property do not
select the calculation or create separate frontend behavior.

A stored Measurement applies from its observation timestamp until the next
Measurement. The mean is the sum of
value times supported duration divided by the *entire* interval duration. Core
returns a mean only for fully supported intervals; otherwise it returns null
with the supported duration and partial/absent status. A complete interval can
have zero new Measurements because a predecessor may support all of it. The
response also identifies the number of new Measurements and the timestamp of
the last one that contributed. That timestamp is never replaced with the
interval label. No interpolation, retrospective ramp, or sensor-quality claim
is implied.

This version has no generic expiry duration. With spontaneous IEC ingestion,
silence can mean either an unchanged value or an outage, and Core cannot infer
which one occurred. Results therefore expose the contributing observation time
instead of turning an arbitrary timeout into a data-validity rule.

Core reads the latest retained predecessor before the requested range and all
Measurements within it. It never fills before the first available Measurement.
Equal values at the same timestamp collapse; conflicting simultaneous values
from different writers fail the query. A value at an interval's end belongs to
the next interval, and subsecond durations contribute exactly. A correction or
backdated insert may change later calculations. InfluxDB's existing point
identity means a correction by the *same* writer at the same timestamp
overwrites that writer's prior value; the overwritten value is not available
to detect a conflict.

The existing 15-minute, hourly, and daily query choices calculate directly
from stored Measurements, not from rounded smaller means. Boundaries remain
half-open and labels use the interval end. Fixed `+01:00` aligns MEZ intervals
without daylight-saving shifts. Core rejects more than 50,000 windows or
100,000 read Measurements rather than calculating from truncated evidence.
The TSTP connector explicitly requests these means and preserves gaps as
missing values; the FE can display the same result or inspect raw values.
TSTP keeps publication batches at 1,000 intervals independently of the query limit.

Core checks read access and representation compatibility before reading
measurements. Existing affine output conversions are applied to complete
means; stored observations are unchanged. The connector library validates
the returned series, range, method, representation, ordered windows and
support evidence before publication.

The observation limit bounds accepted input and client memory, not the
storage engine's internal scan work. `computedAt` is a request-clock instant,
not a write watermark or a transaction spanning metadata and InfluxDB.

## Consequences

- A retained predecessor is necessary for support at the start of a range.
- Every series offers interval charts and raw inspection through the same read interface.
- Carrying the last value is an explicit modelling assumption, not outage detection.
- The contributing observation timestamp exposes age without inventing a generic expiry rule.
- Full mathematical support does not certify a sensor or approve data quality.
- Publication/quality policy, CSV exports, and query scaling remain separate work.
