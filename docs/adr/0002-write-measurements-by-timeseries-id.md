# Write Measurements By TimeSeries Identity

PegelHub's clean measurement write API will require connectors to submit a TimeSeries identifier, observed time, and value. Protocol-specific addresses such as IEC IOAs, FTP column names, station numbers, or channel names may be mapped to TimeSeries identifiers by connector configuration or compatibility adapters, but they are not part of the core Measurement identity.

## Considered Options

- Let connectors write by station number and channel code, then resolve that pair in Core.
- Let connectors write arbitrary field maps and keep using the old Supplier identifier as the time-series grouping key.
- Require connectors to write by TimeSeries identifier and keep protocol addressing outside the core Measurement contract.

## Consequences

Connector setup must know or discover TimeSeries identifiers before writing clean measurements. In exchange, Core receives a stable resource identity, AccessGrant checks become direct, and stored measurements no longer depend on protocol-specific naming.
