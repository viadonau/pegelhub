package at.pegelhub.connector.ma.config;

import at.pegelhub.lib.CoreConnection;

import java.time.Duration;


public record MaConnectorOptions(
        CoreConnection coreConnection,
        Duration delay,
        String mappingsDir
) {
}
