# PegelHub Context

PegelHub models hydrological stations, physical measuring points within those stations, the time series observed at those points, technical connectors that exchange data, and the measurements written into the time-series store.

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
A single observed series at a MeasuringPoint, defined by what is observed and in which unit. A MeasuringPoint can have multiple TimeSeries, such as water level, discharge, water temperature, and air temperature.
_Avoid_: Datastream, channel, measurement series

**Measurement**:
A single value observed for a time series at a specific time.
_Avoid_: Observation, reading, Influx point

**AccessGrant**:
A PegelHub permission that allows a connector to read a station or time series, or write a specific time series.
_Avoid_: Supplier role, Taker role, token permission

**Operator**:
A trusted PegelHub user who maintains the station inventory, connector registrations, time series, and access grants.
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
`discharge`. Clear legacy aliases are normalized at the domain boundary while
unknown trimmed codes are preserved. Measuring-point bank values are either
`left`, `right`, or null. API input and the database schema use only those
canonical values.
