# Keep The Connector Client Interface Hand-Owned

Connectors use the small hand-owned `PegelHubClient` interface. Core OpenAPI may
later generate internal HTTP DTOs or contract guards, but generated Core clients
should not become the connector module's public interface.

## Considered Options

- Generate the full connector library client from Core OpenAPI.
- Keep the current hand-owned connector client and ignore OpenAPI drift.
- Keep the hand-owned connector client interface, while using OpenAPI as a
  contract input for the internal HTTP adapter when the contract is stable
  enough.

## Decision

Keep `PegelHubClient` as the connector-facing seam. It hides OAuth, Core route
shape, request validation, and response envelopes behind a small Measurement
interface that matches connector work: read a TimeSeries window, read latest
Measurement, and write Measurements.

Hiding the response envelope does not mean ignoring its correctness metadata.
The internal HTTP adapter validates that the response belongs to the requested
TimeSeries and rejects a truncated lookback before returning a Measurement
collection. Connector modules therefore receive either the complete requested
lookback or a failure. The specialized latest-value read may accept truncation
because it deliberately requests only the first descending value.

Generated code can sit behind this seam later, but it should be an adapter
implementation detail. Connector modules should not learn the full Core OpenAPI
surface just to exchange Measurements.

## Consequences

The connector library remains responsible for a small amount of hand-maintained
HTTP mapping. That mapping needs contract guards against Core OpenAPI or Core
response fixtures once the measurement/read shape is less volatile. Those
guards include envelope identity and completeness checks inside the adapter.

In exchange, connector callers get a deeper interface with better locality:
Core HTTP churn stays inside `HttpPegelHubClient`, and connector tests keep
targeting hydrological concepts instead of generated transport types.
