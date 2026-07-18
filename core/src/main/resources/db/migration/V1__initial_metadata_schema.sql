create table contact (
    id uuid not null,
    organization varchar(150),
    contact_person varchar(150),
    contact_street varchar(150),
    contact_plz varchar(50),
    location varchar(50),
    contact_country varchar(50),
    emergency_number varchar(50),
    emergency_number_two varchar(50),
    emergency_mail varchar(50),
    service_number varchar(50),
    service_number_two varchar(50),
    service_mail varchar(50),
    administration_phone_number varchar(50),
    administration_phone_number_two varchar(50),
    administration_mail varchar(50),
    contact_nodes varchar(255),
    constraint pk_contact primary key (id)
);

create table connector (
    id uuid not null,
    manufacturer_id uuid not null,
    connector_number varchar(50) not null,
    type_description varchar(100) not null,
    software_version varchar(50) not null,
    works_from_data_version varchar(50) not null,
    data_definition varchar(50) not null,
    software_manufacturer_id uuid not null,
    technically_responsible_id uuid not null,
    operating_company_id uuid not null,
    keycloak_client_id varchar(255),
    status varchar(20),
    nodes varchar(255),
    constraint pk_connector primary key (id),
    constraint uk_connector_keycloak_client_id unique (keycloak_client_id),
    constraint fk_connector_manufacturer foreign key (manufacturer_id) references contact (id),
    constraint fk_connector_software_manufacturer foreign key (software_manufacturer_id) references contact (id),
    constraint fk_connector_technically_responsible foreign key (technically_responsible_id) references contact (id),
    constraint fk_connector_operating_company foreign key (operating_company_id) references contact (id)
);

create table station_owner (
    id uuid not null,
    name varchar(200) not null,
    short_name varchar(80),
    notes varchar(2000),
    constraint pk_station_owner primary key (id)
);

create table station (
    id uuid not null,
    owner_id uuid not null,
    station_number varchar(80) not null,
    name varchar(200) not null,
    water_body varchar(200) not null,
    location varchar(500),
    constraint pk_station primary key (id),
    constraint uk_station_station_number unique (station_number)
);

create table time_series (
    id uuid not null,
    station_id uuid not null,
    observed_property varchar(120) not null,
    unit varchar(40) not null,
    reference_level double precision,
    reference_year integer,
    river_kilometer double precision,
    bank varchar(40),
    rnw double precision,
    hsw double precision,
    mw double precision,
    hw100 double precision,
    external_code varchar(160),
    source_connector_id uuid,
    constraint pk_time_series primary key (id),
    constraint uk_time_series_station_property_unit unique (station_id, observed_property, unit)
);

create table access_grant (
    id uuid not null,
    connector_id uuid not null,
    resource_type varchar(40) not null,
    resource_id uuid not null,
    permission varchar(40) not null,
    constraint pk_access_grant primary key (id)
);
