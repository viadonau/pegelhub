# Parking Lot

Loose project memory for work that should not interrupt the current slice. Keep entries short; when one becomes active, pull it into a branch, issue, or ADR.

- Caddy company/public modes
  Clarify whether deployment needs separate internal-company and public-facing modes, and what differs in routing, TLS, auth, and config.

- Keycloak client provisioning
  Move environment-specific clients out of realm import; keep realm import as local bootstrap and define a clean staging setup path.

- Connector library cleanup
  Remove unused/dead client code, or decide whether shared client pieces should be generated from Core OpenAPI instead of hand-maintained.

- API response shape cleanup
  Standardize response conventions, especially measurement reads, so the API stops exposing domain-ish wrappers accidentally.

- Connector configuration model
  Merge duplicated/legacy config concepts and align connector runtime config with the new Core domain and auth model.

- Caddy/frontend/backend deployment docs
  Tighten wording around deployment modes, env vars, and how frontend, Core, Keycloak, and Caddy fit together.

- Domain migration PR/docs wording
  Clean up task/PR language after the domain migration branch is stable, so the documented intent matches the final shape.

- AccessGrant persistence and Flyway
  Reshape the AccessGrant DB model and introduce a real Flyway migration/baseline story before staging becomes too stateful.

- Influx persistence follow-up
  Collect concrete retention, query performance, and InfluxBucketOperations gateway issues before doing a broader cleanup; the obvious shared Flux query leakage is already reduced.

- Telemetry model shape
  Investigate the remaining legacy Telemetry slice and decide what connector runtime telemetry should look like before deepening its storage/API path.

- Contact mapping collapse
  Remove shallow Contact service/converter indirection and give Contact mapping one clear home instead of repeating 17-field copies.

- Measurement cursor pagination
  Revisit whether Measurement reads need cursor pagination long-term, or whether the GET flow could be simpler with bounded windows and direct limits.

- Connector communicator/runtime cleanup
  Shrink the connector library communicator, delete dead Contact/Connector client methods, and extract shared stamping/config/runtime boilerplate.

- MeasuringPoint domain concept
  Revisit whether a MeasuringPoint should sit between Station and TimeSeries once V1 station/detail workflows are stable.
