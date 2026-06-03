# Keycloak Operations Runbook

This runbook covers the first Keycloak-backed PegelHub auth slice: Core is an OAuth2 resource server, connectors use client credentials, and connector identity is bound in Core by `keycloakClientId`.

## Local Smoke Runbook

Prerequisites:

- `pegelhub-keycloak.test` resolves to `127.0.0.1` on the host.
- `core/.env` exists and matches `core/.env.example`.

Start the local stack:

```sh
bash .agents/skills/pegelhub-local-dev/scripts/pegelhub-local-dev.sh compose-up
```

Request an operator token:

```sh
OPERATOR_TOKEN="$(
  curl -s \
    -d grant_type=client_credentials \
    -d client_id=local-operator \
    -d client_secret=local-dev-operator-secret-change-me \
    http://pegelhub-keycloak.test:8082/realms/pegelhub/protocol/openid-connect/token \
  | jq -r .access_token
)"
```

Register the connector identity in Core:

```sh
curl -i -X POST http://localhost:8080/api/v1/admin/connectors \
  -H "Authorization: Bearer $OPERATOR_TOKEN" \
  -H "Content-Type: application/json" \
  --data '{
    "keycloakClientId": "local-connector-example",
    "status": "ACTIVE",
    "connector": {
      "connectorNumber": "local-connector-example",
      "manufacturer": {"organization": "PegelHub Local"},
      "typeDescription": "local",
      "softwareVersion": "dev",
      "worksFromDataVersion": "dev",
      "dataDefinition": "dev",
      "softwareManufacturer": {"organization": "PegelHub Local"},
      "technicallyResponsible": {"organization": "PegelHub Local"},
      "operationCompany": {"organization": "PegelHub Local"},
      "notes": "local smoke connector"
    }
  }'
```

Request a connector token:

```sh
CONNECTOR_TOKEN="$(
  curl -s \
    -d grant_type=client_credentials \
    -d client_id=local-connector-example \
    -d client_secret=local-dev-connector-secret-change-me \
    http://pegelhub-keycloak.test:8082/realms/pegelhub/protocol/openid-connect/token \
  | jq -r .access_token
)"
```

Smoke expected auth behavior:

```sh
curl -i http://localhost:8080/api/v1/measurement/1h
curl -i -H "Authorization: Bearer $OPERATOR_TOKEN" http://localhost:8080/api/v1/measurement/1h
curl -i -H "Authorization: Bearer $CONNECTOR_TOKEN" http://localhost:8080/api/v1/measurement/systemTime
```

Expected results:

- protected reads without a token return `401`;
- a metadata-only operator token on a measurement route returns `403`;
- public system time remains reachable.

## Production Cutover Checklist

Before cutover:

- Keycloak production host is reachable from Core and connector hosts.
- Realm exists and exposes the issuer URI configured in Core.
- API client `pegelhub-core-api` exists with the required roles.
- Connector service-account clients exist with only the roles they need.
- Core connector records exist with matching `keycloakClientId` values.
- Connector client secrets are installed through the deployment secret mechanism.
- Core `KEYCLOAK_ISSUER_URI` and `PEGELHUB_API_AUDIENCE` are set.
- Keycloak `PEGELHUB_FRONTEND_URL` is set to the deployed frontend origin before initial realm import.
- Management endpoints are internal or protected by infrastructure.
- Keycloak DB backup is complete and a Core rollback release is available.
- Previous connector configs are preserved for forensic comparison, not fallback auth.

During cutover:

- Deploy or verify Keycloak first.
- Deploy Core with resource-server configuration.
- Deploy one connector instance and verify token request plus one write/read smoke.
- Deploy the remaining connectors in small batches.
- Watch Core and connector logs for `401` and `403` spikes.

After cutover:

- Remove stale API-key runbooks from downstream environments.
- Record Keycloak client ids, owners, and rotation dates.
- Schedule a backup restore rehearsal.

## Connector Registration Workflow

For each connector instance:

1. Create a confidential Keycloak client with service accounts enabled.
2. Assign only required API roles, such as `measurement:write` or `telemetry:write`.
3. Install the client id and secret in the connector runtime config.
4. Register the same client id in Core with `POST /api/v1/admin/connectors`.
5. Create or update supplier/taker metadata using the same connector number so Core preserves the registered identity binding.

Connectors must not create their own identity binding. That stays an operator/admin action.

## Secret Rotation

1. Generate a new client secret in Keycloak.
2. Update the connector host secret store.
3. Restart or reload the connector so it requests tokens with the new secret.
4. Verify a connector write/read smoke.
5. Keep the Core connector status `ACTIVE` throughout rotation unless the connector is compromised.

If a connector is compromised, set the Core connector status to `SUSPENDED` and disable or rotate the Keycloak client. Suspension blocks PegelHub-side writes immediately even if an old access token has not expired.

## Token TTL And Revocation

Core validates JWTs offline. Keycloak client disablement or secret rotation prevents future token issuance, but already issued access tokens remain valid until expiry. Keep access-token lifetime short and use Core connector suspension for immediate PegelHub-side blocking.

## Keycloak Database Backup And Restore

For local Docker Compose, Keycloak state lives in the `keycloak-db` Postgres service and `keycloak-data` volume.

Backup example:

```sh
docker compose -f core/docker-compose.yaml exec keycloak-db \
  pg_dump -U keycloak keycloak > keycloak-backup.sql
```

Restore requires an explicit maintenance window and should be rehearsed before production use:

```sh
docker compose -f core/docker-compose.yaml exec -T keycloak-db \
  psql -U keycloak keycloak < keycloak-backup.sql
```

Do not remove Docker volumes or reset production identity data as part of normal rollback.

## Rollback After Auth Cutover

Rollback is a release operation, not a token-table restore:

- keep Keycloak running unless it is the source of outage;
- roll Core back to the previous release only if the previous release is compatible with the current database shape;
- roll connector binaries/config back as a matched set;
- preserve Keycloak clients and Core connector records for investigation;
- after rollback, document which connector ids and secrets were used during the failed cutover.

## Breaking Auth Release Note

PegelHub no longer accepts app-managed API keys or `/api/v1/token` routes. HTTP clients must send `Authorization: Bearer <access-token>` from Keycloak. Connectors must be pre-provisioned with a Keycloak client id and secret, and Core must have a matching connector record with the same `keycloakClientId`.
