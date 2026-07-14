package at.pegelhub.connector.iec.config;

import at.pegelhub.lib.CoreConnection;

import java.net.InetAddress;
import java.time.Duration;


/**
 * IecConnectorSettings spec.
 */
public record IecConnectorSettings(
        CoreConnection coreConnection,
        String mappingsDirectory,
        InetAddress iecHost,
        int iecPort,
        int commonAddress,
        Duration pollInterval
) {
}
