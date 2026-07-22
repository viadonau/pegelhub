create table measuring_point (
    id uuid not null,
    station_id uuid not null,
    name varchar(200) not null,
    reference_level double precision,
    reference_year integer,
    river_kilometer double precision,
    bank varchar(40),
    rnw double precision,
    mw double precision,
    hsw double precision,
    hw100 double precision,
    constraint pk_measuring_point primary key (id),
    constraint fk_measuring_point_station
        foreign key (station_id) references station (id) on delete restrict,
    constraint uk_measuring_point_station_name unique (station_id, name)
);

create index ix_measuring_point_station on measuring_point (station_id);

alter table time_series
    add column measuring_point_id uuid;

with metadata_tuples as (
    select distinct
           station_id,
           reference_level,
           reference_year,
           river_kilometer,
           bank,
           rnw,
           mw,
           hsw,
           hw100
      from time_series
), numbered_tuples as (
    select metadata_tuples.*,
           row_number() over (
               partition by station_id
               order by reference_level nulls first,
                        reference_year nulls first,
                        river_kilometer nulls first,
                        bank nulls first,
                        rnw nulls first,
                        mw nulls first,
                        hsw nulls first,
                        hw100 nulls first) as tuple_number,
           count(*) over (partition by station_id) as tuple_count
      from metadata_tuples
)
insert into measuring_point (
    id,
    station_id,
    name,
    reference_level,
    reference_year,
    river_kilometer,
    bank,
    rnw,
    mw,
    hsw,
    hw100)
select md5(concat_ws(
               ':',
               'pegelhub-measuring-point-v1',
               encode(uuid_send(station_id), 'hex'),
               case
                   when reference_level is null then 'n'
                   else 'v' || encode(float8send(reference_level), 'hex')
               end,
               case
                   when reference_year is null then 'n'
                   else 'v' || encode(int4send(reference_year), 'hex')
               end,
               case
                   when river_kilometer is null then 'n'
                   else 'v' || encode(float8send(river_kilometer), 'hex')
               end,
               case
                   when bank is null then 'n'
                   else 'v' || encode(convert_to(bank, 'UTF8'), 'hex')
               end,
               case
                   when rnw is null then 'n'
                   else 'v' || encode(float8send(rnw), 'hex')
               end,
               case
                   when mw is null then 'n'
                   else 'v' || encode(float8send(mw), 'hex')
               end,
               case
                   when hsw is null then 'n'
                   else 'v' || encode(float8send(hsw), 'hex')
               end,
               case
                   when hw100 is null then 'n'
                   else 'v' || encode(float8send(hw100), 'hex')
               end))::uuid,
       station_id,
       case
           when tuple_count = 1 then station.name
           else left(station.name, 175) || ' / ' || tuple_number
       end,
       reference_level,
       reference_year,
       river_kilometer,
       bank,
       rnw,
       mw,
       hsw,
       hw100
  from numbered_tuples
  join station on station.id = numbered_tuples.station_id;

update time_series
   set measuring_point_id = measuring_point.id
  from measuring_point
 where measuring_point.station_id = time_series.station_id
   and measuring_point.reference_level is not distinct from time_series.reference_level
   and measuring_point.reference_year is not distinct from time_series.reference_year
   and measuring_point.river_kilometer is not distinct from time_series.river_kilometer
   and measuring_point.bank is not distinct from time_series.bank
   and measuring_point.rnw is not distinct from time_series.rnw
   and measuring_point.mw is not distinct from time_series.mw
   and measuring_point.hsw is not distinct from time_series.hsw
   and measuring_point.hw100 is not distinct from time_series.hw100;

alter table time_series
    alter column measuring_point_id set not null,
    drop constraint uk_time_series_station_property_unit,
    drop constraint fk_time_series_station,
    add constraint fk_time_series_measuring_point
        foreign key (measuring_point_id) references measuring_point (id) on delete restrict,
    add constraint uk_time_series_measuring_point_property_unit
        unique (measuring_point_id, observed_property, unit);

alter table time_series
    drop column station_id,
    drop column reference_level,
    drop column reference_year,
    drop column river_kilometer,
    drop column bank,
    drop column rnw,
    drop column mw,
    drop column hsw,
    drop column hw100;
