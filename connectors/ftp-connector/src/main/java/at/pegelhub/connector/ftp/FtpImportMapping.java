package at.pegelhub.connector.ftp;

import java.util.UUID;

public record FtpImportMapping(
        int sourceStationId,
        String sourceParameter,
        UUID targetTimeSeriesId
) {}
