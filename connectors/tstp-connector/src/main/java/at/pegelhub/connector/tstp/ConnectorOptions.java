package at.pegelhub.connector.tstp;

import at.pegelhub.lib.CoreConnection;
import at.pegelhub.lib.config.MappingDirection;

import java.time.Duration;
import java.util.UUID;

public record ConnectorOptions(CoreConnection coreConnection,
                               String tstpAddress, int tstpPort,
                               Duration readDelay, UUID timeSeriesId,
                               int stationId, MappingDirection direction) {}
