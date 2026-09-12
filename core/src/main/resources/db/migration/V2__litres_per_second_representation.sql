alter table time_series drop constraint ck_time_series_source_representation;

alter table time_series add constraint ck_time_series_source_representation check (
    source_representation is null
    or source_representation = 'canonical'
    or (source_representation = 'metres-above-adria' and observed_property = 'water-level')
    or (source_representation = 'litres-per-second' and observed_property = 'discharge')
);
