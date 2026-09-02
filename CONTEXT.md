# PegelHub Context

PegelHub models hydrological stations, physical measuring points within those
stations, the time series observed at those points, technical connectors that
exchange data, and the measurements written into the time-series store.

## Language

**Connector**:
A technical client that authenticates with PegelHub and exchanges station data through a specific protocol or integration.
_Avoid_: Supplier, Taker, API key

**StationOwner**:
The organization or responsibility holder for one or more stations.
_Avoid_: Contact, operator, owner contact

**Station**:
A stable hydrological site, such as a named gauge station on a water body. A Station contains one or more MeasuringPoints.
_Avoid_: Supplier, measurement source

**MeasuringPoint**:
A stable physical observation position within a Station. It groups the TimeSeries observed at the same position and owns shared gauge-location and reference metadata. A MeasuringPoint is not the replaceable sensor or device installed there.
_Avoid_: Sensor, device, measurement value, TimeSeries

**TimeSeries**:
A single observed series at a MeasuringPoint. Its property is one of the
canonical catalog values `water-level`, `water-temperature`, or `discharge`;
the canonical unit is derived from that property.
_Avoid_: Datastream, channel, measurement series

**Measurement**:
A single value observed for a time series at a specific time.
_Avoid_: Observation, reading, Influx point

**Read access**:
An explicit Connector-to-Station or Connector-to-TimeSeries relation that
authorizes measurement reads for the related TimeSeries. A source assignment,
not a read-access relation, authorizes measurement writes.
_Avoid_: AccessGrant, WRITE grant, token permission

**Source assignment**:
The optional Connector and input representation assigned to a TimeSeries. It is
the resource-level write authorization; it does not grant read access.
_Avoid_: WRITE grant, external mapping code

**Lifecycle status**:
The canonical `active` or `inactive` state shared by Connector, Station,
MeasuringPoint, and TimeSeries. The effective monitoring status also reflects
the parent Station and MeasuringPoint.
_Avoid_: Enabled flag, deleted state

**Operator**:
A trusted PegelHub user who reads monitoring views or maintains catalog
metadata and connector access through the operator-facing API. The current web
frontend implements monitoring, not metadata administration.
_Avoid_: Admin, metadata manager

## Monitoring Read Model

The operator monitoring workflow is a small read model over the existing
PostgreSQL metadata services and InfluxDB measurements. It is intentionally a
same-store CQRS read module rather than a second database or a generic query
bus. Monitoring is TimeSeries-based: the catalog contains one row per
TimeSeries and detail routes address a TimeSeries ID. The read model composes
metadata and a bounded latest-measurement window into complete operator
responses; administrative metadata and raw measurement APIs remain available
for their existing clients.

Canonical observed-property values are `water-level`, `water-temperature`, and
`discharge`; unknown values are rejected. Measuring-point bank values are
`left`, `right`, or null. API input accepts known observed-property aliases and
canonicalizes them; persistence and responses use only the canonical values.
Source representations are `canonical` and, for water level,
`metres-above-adria`.
