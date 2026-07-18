package at.pegelhub.connector.ma.config;

import at.pegelhub.connector.ma.core.InputMapping;
import at.pegelhub.lib.config.CoreConnection;

import java.time.Duration;
import java.util.List;

public record MaConnectorConfig(
        CoreConnection coreConnection,
        Duration pollInterval,
        List<InputMapping> mappings
) {}
