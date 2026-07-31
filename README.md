# Pegelhub

This repository contains the Pegelhub core application, connector modules,
deployment setup, and local developer helpers.

## Repository Layout

- `core/`: Spring Boot core application and local core runtime setup.
- `connectors/`: Maven parent module for the connector library plus FTP, ICC,
  IEC, mA, and TSTP connector implementations.
- `deploy/staging/`: Docker Compose based staging stack.
- `deploy/ansible/`: Ansible bootstrap for the staging host.
- `docs/`: project context, architecture notes, and ADRs.
- `.run/`: shared IntelliJ run configurations for repository-level development.
- `scripts/`: repository helper scripts, including `scripts/local-stack.sh` for
  the local Core stack and `scripts/build-connector-image.sh` for connector
  images.

## Common Commands

```bash
mvn -B -ntp -f core/pom.xml test
mvn -B -ntp -f connectors/pom.xml test

test -f core/.env || cp core/.env.example core/.env
scripts/local-stack.sh compose-up
scripts/local-stack.sh smoke
```

Executable Core API examples and client notes live in `core/docs/api/bruno/`.
Keycloak local setup and operations live in `core/docs/keycloak-local-dev.md`
and `core/docs/keycloak-operations.md`.
