package at.pegelhub.connector.ma.config;

import at.pegelhub.lib.CoreConnection;

import java.time.Duration;


public record MaConnectorSettings(
        CoreConnection coreConnection,
        Duration pollInterval,
        String mappingsDirectory
) {
}
