package at.pegelhub.connector.tstp;

import java.time.Duration;

public record ConnectorOptions(String coreAddress, int corePort,
                               String tstpAddress, int tstpPort,
                               Duration readDelay, String propertiesFile) {}
