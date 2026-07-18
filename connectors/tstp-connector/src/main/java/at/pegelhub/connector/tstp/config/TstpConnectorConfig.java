package at.pegelhub.connector.tstp.config;

import at.pegelhub.connector.tstp.TstpMapping;
import at.pegelhub.lib.config.CoreConnection;

import java.time.Duration;

public record TstpConnectorConfig(
        CoreConnection coreConnection,
        TstpServer server,
        Duration pollInterval,
        TstpMapping mapping
) {}
