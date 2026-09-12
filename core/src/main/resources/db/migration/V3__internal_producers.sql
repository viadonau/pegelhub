create table internal_producer (
    id uuid primary key,
    output_time_series_id uuid unique references time_series(id) on delete restrict
);
create table internal_producer_input (
    producer_id uuid not null references internal_producer(id) on delete cascade,
    time_series_id uuid not null references time_series(id) on delete restrict,
    primary key (producer_id, time_series_id)
);
alter table time_series add column source_internal_producer_id uuid references internal_producer(id) on delete restrict;
alter table time_series drop constraint ck_time_series_source_pair;
alter table time_series add constraint ck_time_series_source_pair check (
    (source_connector_id is null and source_internal_producer_id is null and source_representation is null)
    or (source_connector_id is not null and source_internal_producer_id is null and source_representation is not null)
    or (source_connector_id is null and source_internal_producer_id is not null and source_representation = 'canonical')
);
create unique index uk_time_series_internal_producer on time_series(source_internal_producer_id)
    where source_internal_producer_id is not null;
