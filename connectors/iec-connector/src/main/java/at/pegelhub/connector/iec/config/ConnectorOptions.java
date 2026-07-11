package at.pegelhub.connector.iec.config;

import at.pegelhub.lib.CoreConnection;

import java.net.InetAddress;
import java.time.Duration;


/**
 * ConnectorOptions spec.
 */
public record ConnectorOptions(
        CoreConnection coreConnection,
        String mappingsDir,
        InetAddress iecHost,
        int iecPort,
        int commonAddress,
        Duration delay
) {
}
