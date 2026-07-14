package at.pegelhub.connector.ftp;

import at.pegelhub.connector.ftp.fileparsing.ParserType;
import at.pegelhub.lib.CoreConnection;

import java.net.InetAddress;
import java.time.Duration;
import java.util.UUID;

public record FtpConnectorSettings(CoreConnection coreConnection,
                               InetAddress ftpAddress, int ftpPort,
                               String username, String password,
                               String path, ParserType parserType,
                               Duration pollInterval, UUID timeSeriesId,
                               int stationId, String parameter) {}
