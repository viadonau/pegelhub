# Flyway Metadata Schema

Core owns the PostgreSQL metadata schema through the single clean baseline in
`src/main/resources/db/migration/V1__initial_metadata_schema.sql`. Hibernate
runs with `ddl-auto=validate`; it never creates or updates metadata tables.

This V2 development line intentionally resets metadata. Measurements are keyed
by TimeSeries UUID, so local and staging PostgreSQL and InfluxDB volumes must be
recreated together unless an explicit ID-preserving migration is performed. Do
not apply this baseline to a database whose data must be preserved without such
a migration plan.

The baseline creates the `StationOwner -> Station -> MeasuringPoint ->
TimeSeries` hierarchy, minimal Connector metadata, exact numeric point fields,
catalog/status/source checks, and explicit Connector station/time-series read
access tables. There are no Contact, AccessGrant, compatibility, or hard-delete
migrations.

For a local reset:

```sh
docker compose --env-file core/.env -f core/docker-compose.yaml down -v
docker compose --env-file core/.env -f core/docker-compose.yaml up -d
```

Confirm the Core health endpoint and load canonical demo metadata before
running the frontend smoke workflow.
