# PegelHub Agents

Solo-developed Java 21 Maven repo. Keep changes focused and improve touched code without treating current class-level conventions as authoritative.

## Shape

- `core/`: Spring Boot app, local Docker Compose stack, API docs, tests.
- `connectors/`: connector parent, shared library, connector implementations.

## Code Direction

- Prefer records for new DTOs, projections, configuration properties, and small immutable value objects.
- Put Jakarta Bean Validation annotations on API DTO record components and use `@Valid` at controller/input boundaries.
- Use compact record constructors only for local invariants/normalization; keep business validation in services.
- Prefer JPA repositories for Postgres; use JDBC only with a strong reason and discuss the tradeoff first.
- API contracts may be improved when useful; make breaking changes intentional and call them out.

## Working Rules

- Improve only within the task's natural scope; mention broader cleanup as follow-up.
- Preserve user work. Never revert unrelated changes unless explicitly asked.
- Do not create TODO files, issues, or tracking docs unless asked.

## Local Dev And Safety

- Use `$pegelhub-local-dev` for core stack, Docker Compose, `.env`, health, smoke, and core build/test work.
- Start diagnostics compactly: `bash .agents/skills/pegelhub-local-dev/scripts/pegelhub-local-dev.sh status`.
- Core checks: `bash .agents/skills/pegelhub-local-dev/scripts/pegelhub-local-dev.sh test-core` or `build-core`.
- Connector checks: use the nearest Maven module, e.g. `mvn -f connectors/<module>/pom.xml test`.
- Do not remove Docker volumes, reset databases, or run `docker compose down -v` without explicit approval.
- Prefer read-only health/smoke/GET checks; connector-to-core runtime checks may create measurements.

## Testing

- Run the narrowest meaningful test first; broaden when touching persistence, API contracts, shared code, or risky behavior.
- If tests cannot run, state why and name the command that would verify the change.
