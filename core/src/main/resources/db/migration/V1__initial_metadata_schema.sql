create table station_owner (
    id uuid primary key,
    name varchar(200) not null,
    short_name varchar(80),
    notes varchar(2000)
);

create table connector (
    id uuid primary key,
    name varchar(200) not null,
    type varchar(30) not null,
    keycloak_client_id varchar(255),
    status varchar(8) not null default 'active',
    constraint ck_connector_status check (status in ('active', 'inactive')),
    constraint ck_connector_type check (type in ('ftp', 'tstp', 'iec', 'icc', 'ma', 'other')),
    constraint uk_connector_keycloak_client_id unique (keycloak_client_id)
);

create table station (
    id uuid primary key,
    owner_id uuid not null references station_owner(id) on delete restrict,
    name varchar(200) not null,
    water_body varchar(200) not null,
    status varchar(8) not null default 'active',
    constraint ck_station_status check (status in ('active', 'inactive'))
);

create table measuring_point (
    id uuid primary key,
    station_id uuid not null references station(id) on delete restrict,
    name varchar(200) not null,
    status varchar(8) not null default 'active',
    river_kilometer numeric,
    bank varchar(8),
    latitude numeric,
    longitude numeric,
    gauge_zero_elevation_m_above_adria numeric,
    reference_set_year integer,
    rnw_cm numeric,
    mw_cm numeric,
    hsw_cm numeric,
    hw100_cm numeric,
    constraint ck_measuring_point_status check (status in ('active', 'inactive')),
    constraint ck_measuring_point_bank check (bank in ('left', 'right') or bank is null),
    constraint ck_measuring_point_river_kilometer check (river_kilometer is null or river_kilometer >= 0),
    constraint ck_measuring_point_coordinates check (
        (latitude is null and longitude is null)
        or (latitude is not null and longitude is not null
            and latitude between -90 and 90 and longitude between -180 and 180)
    ),
    constraint ck_measuring_point_reference_set check (
        (reference_set_year is null and rnw_cm is null and mw_cm is null and hsw_cm is null and hw100_cm is null)
        or (reference_set_year is not null and reference_set_year between 1 and 9999
            and (rnw_cm is not null or mw_cm is not null or hsw_cm is not null or hw100_cm is not null))
    ),
    constraint uk_measuring_point_station_name unique (station_id, name)
);

create table time_series (
    id uuid primary key,
    measuring_point_id uuid not null references measuring_point(id) on delete restrict,
    observed_property varchar(40) not null,
    status varchar(8) not null default 'active',
    source_connector_id uuid references connector(id) on delete restrict,
    source_representation varchar(32),
    constraint ck_time_series_status check (status in ('active', 'inactive')),
    constraint ck_time_series_property check (observed_property in ('water-level', 'water-temperature', 'discharge')),
    constraint ck_time_series_source_pair check ((source_connector_id is null) = (source_representation is null)),
    constraint ck_time_series_source_representation check (
        source_representation in ('canonical', 'metres-above-adria')
        and (source_representation = 'canonical' or observed_property = 'water-level')
        or source_representation is null
    ),
    constraint uk_time_series_measuring_point_property unique (measuring_point_id, observed_property)
);

create index ix_station_owner on station(owner_id);
create index ix_measuring_point_station on measuring_point(station_id);
create index ix_time_series_measuring_point on time_series(measuring_point_id);
create index ix_time_series_source_connector on time_series(source_connector_id);

create table connector_station_read_access (
    connector_id uuid not null references connector(id) on delete restrict,
    station_id uuid not null references station(id) on delete restrict,
    primary key (connector_id, station_id)
);

create table connector_time_series_read_access (
    connector_id uuid not null references connector(id) on delete restrict,
    time_series_id uuid not null references time_series(id) on delete restrict,
    primary key (connector_id, time_series_id)
);
