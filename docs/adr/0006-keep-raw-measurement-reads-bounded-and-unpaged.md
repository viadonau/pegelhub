# Keep Raw Measurement Reads Bounded and Unpaged

Raw Measurement reads on
`GET /api/v1/time-series/{timeSeriesId}/measurements` use a bounded relative
or explicit time window, ordering, and a result limit. They do not provide
continuation pagination.

Chart-oriented reads continue to use the bucket endpoint. Latest-value reads
continue to use the raw endpoint with `order=desc&limit=1`.

## Considered Options

- Keep cursor pagination for exhaustive traversal of raw Measurement windows.
- Keep raw reads bounded and unpaged, with truncation reported to callers.
- Add separate latest-value or export endpoints.

## Decision

Keep raw Measurement reads bounded and unpaged. Callers select either a
relative window with `last` or an explicit `from` and `to` window, together
with `order` and `limit`.

Core fetches one row beyond the requested limit so the response can report
`truncated=true` when more raw values exist in the selected window. A caller
that needs a complete result must narrow the time window or use a larger
allowed limit.

Results remain deterministically ordered by `observedAt` and
`submittedByConnectorId` so values with the same observation time have a
stable order. That ordering is not exposed as a continuation cursor.

Do not add a dedicated latest endpoint in this slice. `order=desc&limit=1`
keeps "latest" as a shallow query variant of the raw Measurement read instead
of a second interface to maintain.

## Consequences

The raw Measurement response keeps the `truncated` field and removes the
`next` continuation object. The request no longer accepts a `cursor`.

Interactive clients can show the bounded result and ask operators to narrow the
window when it is truncated. The internal `PegelHubClient` HTTP adapter detects
truncation for complete-lookback reads and fails the call before exposing its
hand-owned Measurement collection to connector modules. Latest-value reads are
the deliberate exception because `order=desc&limit=1` asks for exactly one
value rather than a complete window.

Exhaustive exports and reconciliation are not current requirements. A cursor or
a dedicated export workflow can be introduced when a concrete consumer needs
one.

Measurement read tests should keep covering bounded relative and explicit
windows, limits, truncation, duplicate observed-time ordering, descending
reads, latest-value reads, and response contract shape.
