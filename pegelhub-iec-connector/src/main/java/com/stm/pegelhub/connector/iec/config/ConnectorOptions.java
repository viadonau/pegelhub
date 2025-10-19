package com.stm.pegelhub.connector.iec.config;

import java.net.InetAddress;
import java.time.Duration;


/**
 * ConnectorOptions spec.
 */
public record ConnectorOptions(
        String dataPointsDir,
        String coreAddress,
        int corePort,
        InetAddress iec_host,
        int iec_port,
        int common_address,
        Duration delay
) {
}
