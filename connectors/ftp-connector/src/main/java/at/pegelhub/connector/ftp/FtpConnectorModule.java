package at.pegelhub.connector.ftp;

import at.pegelhub.connector.ftp.config.FtpConnectorConfig;
import at.pegelhub.connector.ftp.config.FtpConnectorConfigLoader;
import at.pegelhub.connector.ftp.fileparsing.ParserFactory;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.PegelHubClientFactory;
import at.pegelhub.lib.config.ConnectorConfigDirectory;
import at.pegelhub.lib.runtime.ConnectorModule;
import at.pegelhub.lib.runtime.ConnectorRuntimeAssembly;
import at.pegelhub.lib.runtime.ConnectorRuntimeDefinition;
import org.apache.commons.net.ftp.FTPClient;

import java.time.Duration;

public final class FtpConnectorModule implements ConnectorModule {
    private final FtpConnectorConfigLoader configLoader = new FtpConnectorConfigLoader();

    @Override
    public String name() {
        return "FTP Connector";
    }

    @Override
    public ConnectorRuntimeDefinition define(
            ConnectorConfigDirectory configDirectory,
            PegelHubClientFactory coreClients) throws Exception {
        FtpConnectorConfig config = configLoader.load(configDirectory);
        try (ConnectorRuntimeAssembly runtime = ConnectorRuntimeAssembly.begin(name())) {
            FTPClient ftp = new FTPClient();
            runtime.own(ftp::disconnect);
            ftp.setControlKeepAliveTimeout(Duration.ofMinutes(15));
            ftp.setDataTimeout(Duration.ofMinutes(15));

            PegelHubClient client = runtime.own(coreClients.create(config.coreConnection()));

            runtime.fixedDelayTask("ftp-poll", new FtpImportJob(
                            ftp,
                            config,
                            client,
                            ParserFactory.getParser(config.source().parserType())), config.pollInterval());
            return runtime.complete();
        }
    }
}
