package at.pegelhub.connector.icc.config;

import at.pegelhub.connector.icc.IccMapping;
import at.pegelhub.lib.config.CoreConnection;

import java.time.Duration;
import java.util.List;

public record IccConnectorConfig(
        CoreConnection localCore,
        CoreConnection remoteCore,
        Duration pollInterval,
        List<IccMapping> mappings
) {}
