package at.pegelhub.connector.ftp.config;

import at.pegelhub.connector.ftp.FtpImportMapping;
import at.pegelhub.lib.config.CoreConnection;

import java.time.Duration;

public record FtpConnectorConfig(
        CoreConnection coreConnection,
        FtpServer server,
        FtpSource source,
        Duration pollInterval,
        FtpImportMapping mapping
) {}
