package at.pegelhub.connector.iec.config;

import at.pegelhub.connector.iec.datapoints.DataPointMapping;
import at.pegelhub.lib.config.CoreConnection;

import java.time.Duration;
import java.util.List;

public record IecConnectorConfig(
        CoreConnection coreConnection,
        IecServer server,
        Duration pollInterval,
        List<DataPointMapping> mappings
) {}
