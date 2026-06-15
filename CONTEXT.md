# PegelHub Context

PegelHub models hydrological stations, the time series observed at those stations, technical connectors that exchange data, and the measurements written into the time-series store.

## Language

**Connector**:
A technical client that authenticates with PegelHub and exchanges station data through a specific protocol or integration.
_Avoid_: Supplier, Taker, API key

**StationOwner**:
The organization or responsibility holder for one or more stations.
_Avoid_: Contact, operator, owner contact

**Station**:
A stable hydrological place where values are observed, such as a gauge location on a water body.
_Avoid_: Supplier, measurement source

**TimeSeries**:
A single observed series at a station, defined by what is observed and in which unit. A station can have multiple time series, such as water level, discharge, water temperature, and air temperature.
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
