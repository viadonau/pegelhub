create table notification_destination (
    id uuid primary key,
    configuration jsonb not null,
    enabled boolean not null
);
create table notification_delivery (
    id uuid primary key,
    request_id uuid not null,
    source_id uuid,
    destination_id uuid not null references notification_destination(id) on delete restrict,
    route jsonb not null,
    subject varchar(256) not null,
    body text not null,
    state varchar(12) not null check (state in ('PENDING','SENDING','ACCEPTED','FAILED','CANCELLED')),
    attempts integer not null default 0 check (attempts between 0 and 5),
    created_at timestamptz not null,
    next_attempt_at timestamptz not null,
    completed_at timestamptz,
    last_error varchar(300),
    lease_token uuid,
    lease_until timestamptz,
    unique(source_id, destination_id)
);
create index ix_delivery_due on notification_delivery(next_attempt_at) where state in ('PENDING','SENDING');
create index ix_delivery_history on notification_delivery(created_at desc, id);
create index ix_delivery_source on notification_delivery(source_id);
create table notification_snmp_engine (
    id integer primary key check (id = 1),
    engine_id bytea not null,
    boots integer not null check (boots between 1 and 2147483647)
);
