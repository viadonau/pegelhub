# Flyway Metadata Schema

Core owns the PostgreSQL metadata schema through the ordered migrations in
`src/main/resources/db/migration/`. `V1__initial_metadata_schema.sql` creates
the current metadata model; `V2__litres_per_second_representation.sql` extends
the discharge source-representation constraint. Hibernate runs with
`ddl-auto=validate`; it never creates or updates metadata tables.

The current metadata model introduced a clean baseline in place of the legacy
schema. Measurements are keyed by TimeSeries UUID, so a deliberate legacy
reset must recreate the PostgreSQL and InfluxDB dataset together unless an
explicit ID-preserving migration is performed. Do not apply the baseline to a
database whose data must be preserved without such a migration plan. Normal
upgrades of an already migrated database apply pending migrations without
resetting data.

The baseline creates the `StationOwner -> Station -> MeasuringPoint ->
TimeSeries` hierarchy, minimal Connector metadata, exact numeric point fields,
catalog/status/source checks, and explicit Connector station/time-series read
access tables. There are no Contact, AccessGrant, compatibility, or hard-delete
migrations.

Only for a disposable local reset: the following commands delete all Compose
volumes, including the Keycloak database. Do not use them as an upgrade procedure
or against a dataset that must be preserved.

```sh
docker compose --env-file core/.env -f core/docker-compose.yaml down -v
docker compose --env-file core/.env -f core/docker-compose.yaml up -d
```

Confirm the Core health endpoint and load canonical demo metadata before
running the frontend smoke workflow.
