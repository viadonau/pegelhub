# Write Measurements By TimeSeries Identity

## Status

Accepted and implemented.

PegelHub's measurement write API requires connectors to submit a TimeSeries
identifier, observed time, and value. Protocol-specific addresses such as IEC
IOAs, FTP column names, station numbers, or channel names map to TimeSeries
identifiers in connector configuration; they are not part of the Core
Measurement identity.

## Considered Options

- Let connectors write by station number and channel code, then resolve that pair in Core.
- Let connectors write arbitrary field maps and keep using the old Supplier identifier as the time-series grouping key.
- Require connectors to write by TimeSeries identifier and keep protocol addressing outside the core Measurement contract.

## Consequences

Connector setup must know or discover TimeSeries identifiers before writing clean measurements. In exchange, Core receives a stable resource identity, source-assignment and read-access checks remain direct, and stored measurements no longer depend on protocol-specific naming.
