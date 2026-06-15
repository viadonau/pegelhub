package at.pegelhub.connector.tstp;

import java.time.Duration;
import java.util.UUID;

public record ConnectorOptions(String coreAddress, int corePort,
                               String tstpAddress, int tstpPort,
                               Duration readDelay, UUID timeSeriesId,
                               String propertiesFile) {}
