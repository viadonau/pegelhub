# Parking Lot

Loose project memory for work that should not interrupt the current slice. Keep entries short; when one becomes active, pull it into a branch, issue, or ADR.

- Caddy company/public modes
  Clarify whether deployment needs separate internal-company and public-facing modes, and what differs in routing, TLS, auth, and config.

- Keycloak connector client provisioning
  Automate environment-specific connector enrollment and secret rotation only
  when repeated operations justify it; staging currently uses a documented
  manual enrollment step.

- Connector library cleanup
  Decide whether the remaining shared client pieces should be generated from Core OpenAPI instead of hand-maintained.

- API response shape cleanup
  Standardize response conventions, especially measurement reads, so the API stops exposing domain-ish wrappers accidentally.

- Request DTO Bean Validation
  Replace constructor-only checks and hand-authored OpenAPI requiredness with Jakarta Bean Validation (`@NotNull`, `@NotBlank`, `@Size`, and nested `@Valid`) at controller boundaries, then let Springdoc derive required and nullable request-schema constraints from the enforced runtime contract.

- Connector configuration model
  Merge duplicated/legacy config concepts and align connector runtime config with the new Core domain and auth model.

- Connector mapping execution and diagnostics
  Standardize startup validation, semantic mapping identity, per-mapping failure isolation, cycle summaries, and runtime error reporting across FTP, ICC, IEC, mA, and TSTP. Decide whether configuration source names remain available at runtime before introducing a shared mapping wrapper.

- Caddy/frontend/backend deployment docs
  Tighten wording around deployment modes, env vars, and how frontend, Core, Keycloak, and Caddy fit together.

- Shared Compose base
  Extract genuinely common local and staging service definitions into a shared Compose base with thin environment-specific overrides, so database, InfluxDB, Keycloak, and Core changes have one source of truth while local and staging policy remains explicit.

- Staging container logging policy
  Investigate whether log rotation should remain explicit in each Compose file or move to host-wide Docker daemon defaults, including the Ansible ownership and migration impact for existing containers.

- Domain migration PR/docs wording
  Clean up task/PR language after the domain migration branch is stable, so the documented intent matches the final shape.

- AccessGrant persistence and Flyway
  Reshape the AccessGrant DB model and introduce a real Flyway migration/baseline story before staging becomes too stateful.

- Influx persistence follow-up
  Collect concrete query performance and InfluxBucketOperations gateway issues before doing a broader cleanup; the obvious shared Flux query leakage is already reduced.

- Telemetry model shape
  Investigate the remaining legacy Telemetry slice and decide what connector runtime telemetry should look like before deepening its storage/API path.

- Contact mapping collapse
  Remove shallow Contact service/converter indirection and give Contact mapping one clear home instead of repeating 17-field copies.

- MeasuringPoint domain concept
  Revisit whether a MeasuringPoint should sit between Station and TimeSeries once V1 station/detail workflows are stable.
