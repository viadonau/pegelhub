create table quality_profile (
    id uuid primary key references internal_producer(id) on delete restrict,
    configuration jsonb not null,
    enabled boolean not null,
    next_run_at timestamptz not null,
    current_run_id uuid,
    run_requested boolean not null default false
);
create unique index uk_quality_profile_name on quality_profile(lower(configuration->>'name'));
create table quality_run (
    id uuid primary key,
    profile_id uuid not null references quality_profile(id) on delete restrict,
    configuration jsonb not null,
    state varchar(12) not null check (state in ('RUNNING','PASSED','FINDINGS','INCOMPLETE','ERROR','INTERRUPTED')),
    started_at timestamptz not null,
    completed_at timestamptz,
    finding_count integer not null default 0,
    output_state varchar(16) not null check (output_state in ('NOT_CONFIGURED','SUPPRESSED','PENDING','WRITTEN','FAILED','INTERRUPTED')),
    error varchar(300)
);
create index ix_quality_run_history on quality_run(started_at desc, id);
create index ix_quality_run_profile on quality_run(profile_id, started_at desc, id);
create table quality_finding (
    id bigint generated always as identity primary key,
    run_id uuid not null references quality_run(id) on delete cascade,
    finding jsonb not null
);
create index ix_quality_finding_run on quality_finding(run_id, id);
