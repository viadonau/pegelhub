alter table station
    add constraint fk_station_owner
        foreign key (owner_id) references station_owner (id) on delete restrict;

alter table time_series
    add constraint fk_time_series_station
        foreign key (station_id) references station (id) on delete restrict,
    add constraint fk_time_series_source_connector
        foreign key (source_connector_id) references connector (id) on delete restrict;

alter table access_grant
    add constraint fk_access_grant_connector
        foreign key (connector_id) references connector (id) on delete restrict,
    add constraint uk_access_grant_assignment
        unique (connector_id, resource_type, resource_id, permission);

create index ix_station_owner on station (owner_id);
create index ix_time_series_source_connector on time_series (source_connector_id);
create index ix_access_grant_connector on access_grant (connector_id);
create index ix_access_grant_resource on access_grant (resource_type, resource_id);

-- access_grant.resource_id is polymorphic and cannot use one ordinary foreign key.
-- The time-series uniqueness constraint already has station_id as its leftmost column.
