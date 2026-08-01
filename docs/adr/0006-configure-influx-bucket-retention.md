# Configure InfluxDB Bucket Retention At Deployment

PegelHub will configure retention separately for measurement data and technical
telemetry when the Compose stack starts.

## Considered Options

- Keep both application buckets at InfluxDB's infinite default retention.
- Delete old values from application code on a schedule.
- Configure native InfluxDB retention policies during infrastructure startup.

## Decision

The measurement and telemetry buckets each have one deployment setting. Both
default to `60d`. The explicit value `0s` means infinite retention; otherwise,
the value must be a positive whole number of hours, days, or weeks.

A one-shot Compose service creates missing application buckets and reconciles
the configured retention on existing buckets before Core starts. InfluxDB owns
expiry and deletion. Core does not expose retention through its API or run a
cleanup scheduler.

The internal bucket created by the official InfluxDB image is not changed.
PostgreSQL metadata is also outside this policy. `INFLUX_LATEST_RANGE` remains
an independent query horizon and does not control data expiry.

## Consequences

Operators can retain either data category indefinitely without adding another
enable flag. Reducing a finite retention or changing infinite retention to a
finite value can permanently remove older data once InfluxDB enforces the new
policy. Rolling back the application image does not restore expired data.

This decision adds no archive or rollup tier. Those can be introduced later if
long-term history becomes a requirement.
