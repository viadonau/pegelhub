# ADR 0007: Keep PegelHub in One Repository with Separate Deployables

## Status

Accepted

## Context

PegelHub has one maintainer and one product boundary, but its browser frontend
and server-side Core use different languages, toolchains, and runtime images.
They change together at the HTTP contract, local environment, Keycloak setup,
and staging topology. Keeping the source in two repositories creates a second
coordination boundary without providing separate ownership or access-control
needs.

## Decision

Keep the frontend and backend in one repository:

```text
pegelhub/
  core/
  connectors/
  frontend/
  deploy/
  docs/
```

The Java and Angular applications remain separate deployables:

- Maven continues to build only `core` and `connectors` through the root
  aggregator POM.
- Angular continues to use its own `package.json`, npm lockfile, Dockerfile,
  tests, and Node toolchain under `frontend/`.
- Core and frontend keep separate container images, health checks, delivery
  workflows and scripts, releases, and rollback state.
- HTTP and OpenAPI remain the integration seam. The frontend does not import
  backend implementation classes or persistence models.
- CI uses path filters so frontend-only changes do not publish backend images,
  while pull-request checks can still verify both applications and the shared
  staging configuration.

## Consequences

Changes to API contracts, authentication, runtime configuration, and staging
can be reviewed atomically. Local development and documentation have one
canonical checkout, and deployment configuration no longer needs a
cross-repository dispatch token.

The frontend source is imported as a snapshot under the `frontend/` path. Its
earlier history remains available in the archived frontend repository and the
migration bundle instead of becoming part of the backend repository. The
repository is larger, and CI must explicitly preserve independent Java and
Node workflows rather than treating the monorepo as one build artifact.

## Rejected Alternatives

- **Keep two repositories:** preserves independent history, but retains
  coordination overhead for a single maintainer and one product.
- **Build Angular through Maven:** couples unrelated toolchains and hides the
  frontend's natural npm/Docker interface behind a shallow wrapper.
- **Package Angular inside the Spring Boot JAR:** removes useful deployment and
  rollback independence and makes the runtime topology less explicit.
