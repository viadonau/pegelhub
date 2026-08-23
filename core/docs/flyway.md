# Flyway Metadata Schema Rollout

Core owns the PostgreSQL metadata schema through the migrations in
`src/main/resources/db/migration/`. Hibernate uses `ddl-auto=validate` and must
not create or update the schema.

Fresh databases keep `FLYWAY_BASELINE_ON_MIGRATE=false`. Flyway applies V1,
V2, V3, and every later migration before Hibernate validates the result.

Migration histories are immutable per deployed lineage. A database that has
already applied migrations from another branch must be started with an artifact
that still contains those exact migration files and checksums plus the new
migration. Do not use `flyway repair` to make two different version histories
look compatible.

V2 adds these explicitly named foreign keys with `ON DELETE RESTRICT`:

- `fk_station_owner`: `station.owner_id` to `station_owner.id`
- `fk_time_series_station`: `time_series.station_id` to `station.id`
- `fk_time_series_source_connector`: `time_series.source_connector_id` to
  `connector.id`; null remains allowed
- `fk_access_grant_connector`: `access_grant.connector_id` to `connector.id`

V2 also adds the access-grant assignment uniqueness constraint and its connector
and resource lookup indexes. Keeping these definitions in V2 means fresh and
baselined databases receive the same access-grant integrity rules exactly once.

V3 introduces `measuring_point` between Station and TimeSeries. It groups legacy
TimeSeries rows by Station plus their complete shared gauge-metadata tuple,
backfills one deterministic point per tuple using a canonical binary encoding
that is independent of PostgreSQL float formatting, replaces
`time_series.station_id` with `measuring_point_id`, and moves the shared gauge
columns to the new table. The migration replaces `fk_time_series_station` with
`fk_time_series_measuring_point`; Station ancestry remains available through
`measuring_point.station_id` and `fk_measuring_point_station`.

No relationship cascades on delete. Existing dependent rows must be handled
explicitly before their parent can be deleted. `access_grant.resource_id` has no
ordinary foreign key because its target depends on `resource_type`.

## Existing Hibernate-Created Database

`FLYWAY_BASELINE_ON_MIGRATE=true` is a one-time adoption switch, not a normal
runtime setting. It tells Flyway to record a non-empty schema as version 1 and
then apply V2 and later migrations. Never enable it before inspecting the live
schema.

1. Stop Core so metadata cannot change during the preflight, and take a database
   backup. Keep the database service running. For the local stack:

   ```sh
   docker compose --env-file core/.env -f core/docker-compose.yaml stop core-app
   docker compose --env-file core/.env -f core/docker-compose.yaml \
     exec -T meta-db sh -c 'pg_dump -U postgres "$POSTGRES_DB"' \
     > metadata-before-flyway.sql
   ```

   Use `/etc/pegelhub/staging/pegelhub.env` with
   `deploy/single-host/compose.yaml` on staging.

2. Open `psql` and confirm that Flyway has not already adopted the schema:

   ```sh
   docker compose --env-file core/.env -f core/docker-compose.yaml \
     exec meta-db sh -c 'psql -U postgres -d "$POSTGRES_DB"'
   ```

   ```sql
   select to_regclass('public.flyway_schema_history');
   ```

   If this returns a table name, do not baseline again. Leave the flag false and
   inspect the existing migration history instead.

3. Compare the live tables and columns with
   `V1__initial_metadata_schema.sql`. At minimum, inspect all metadata tables and
   check for conflicting constraint names:

   ```text
   \dt public.*
   \d+ public.contact
   \d+ public.connector
   \d+ public.station_owner
   \d+ public.station
   \d+ public.time_series
   \d+ public.access_grant
   ```

   ```sql
   select conname, pg_get_constraintdef(oid)
     from pg_constraint
    where connamespace = 'public'::regnamespace
      and conname in (
        'fk_station_owner',
        'fk_time_series_station',
        'fk_time_series_source_connector',
        'fk_access_grant_connector'
      )
    order by conname;
   ```

   Stop and reconcile any schema difference or conflicting constraint before
   continuing.

4. Confirm that V2 will not encounter orphaned relationship values:

   ```sql
   select 'station.owner_id -> station_owner.id' as relationship, count(*) as orphan_count
     from station child
     left join station_owner parent on parent.id = child.owner_id
    where parent.id is null
   union all
   select 'time_series.station_id -> station.id', count(*)
     from time_series child
     left join station parent on parent.id = child.station_id
    where parent.id is null
   union all
   select 'time_series.source_connector_id -> connector.id', count(*)
     from time_series child
     left join connector parent on parent.id = child.source_connector_id
    where child.source_connector_id is not null and parent.id is null
   union all
   select 'access_grant.connector_id -> connector.id', count(*)
     from access_grant child
     left join connector parent on parent.id = child.connector_id
    where parent.id is null;
   ```

   Every count must be zero. Back up and correct orphaned data deliberately;
   V2 will reject it rather than delete or rewrite it.

5. Set `FLYWAY_BASELINE_ON_MIGRATE=true` in the stack's `.env` file for the
   first Flyway-managed startup only. Start Core with the normal stack command:

   ```sh
   scripts/local-stack.sh compose-up
   ```

   On staging, run the normal checked deployment with the intended
   release-specific image tag:

   ```sh
   deploy/single-host/scripts/deploy.sh <image-tag>
   ```

6. Confirm that Core starts successfully, then inspect the history table:

   ```sql
   select installed_rank, version, description, type, success
     from flyway_schema_history
    order by installed_rank;
   ```

   An adopted schema must have a successful `BASELINE` row at version 1 and
   successful SQL migration rows through version 3. Also check the Core logs for
   a successful Flyway migration and Hibernate startup. If startup fails, keep
   Core stopped and reconcile the failure or restore the backup before proceeding.

7. Immediately set `FLYWAY_BASELINE_ON_MIGRATE=false` again and recreate Core so
   the normal safe setting is active. Run `scripts/local-stack.sh compose-up`
   locally or repeat the staging deploy with the same image tag. Confirm health
   and verify that the migration history is unchanged.
