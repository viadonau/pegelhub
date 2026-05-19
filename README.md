# Pegelhub

This repository contains the Pegelhub core application and connector modules.

## Repository Layout

- `core/`: Spring Boot core application and local core runtime setup.
- `connectors/`: connector library and connector implementations.
- `.run/`: shared IntelliJ run configurations for repository-level development.
- `scripts/`: repository helper scripts such as `scripts/worktree-start.sh` and `scripts/worktree-clean.sh` for managing task worktrees.

Core API client documentation and the Postman collection live in `core/docs/api/postman/`. Keycloak local setup and operations live in `core/docs/keycloak-local-dev.md` and `core/docs/keycloak-operations.md`.
