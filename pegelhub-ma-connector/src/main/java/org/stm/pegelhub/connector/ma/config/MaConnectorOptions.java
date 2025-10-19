package org.stm.pegelhub.connector.ma.config;

import java.time.Duration;


public record MaConnectorOptions(
        String coreAddress,
        int corePort,
        Duration delay,
        String inputsDir
) {
}