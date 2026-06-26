package at.pegelhub.connector.ftp;

import at.pegelhub.connector.ftp.fileparsing.ParserType;

import java.net.InetAddress;
import java.time.Duration;
import java.util.UUID;

public record ConnectorOptions(InetAddress coreAddress, int corePort,
                               InetAddress pegelAddress, int pegelPort,
                               String username, String password,
                               String path, ParserType parserType,
                               Duration readDelay, UUID timeSeriesId,
                               String parameter,
                               String propertiesFile) {}
