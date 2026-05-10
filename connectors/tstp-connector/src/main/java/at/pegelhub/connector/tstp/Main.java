package at.pegelhub.connector.tstp;

import at.pegelhub.connector.tstp.service.TstpConfigService;
import at.pegelhub.connector.tstp.service.impl.TstpConfigServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final String DEFAULT_CONFIG_DIR = "/app/config";
    private static final String CONNECTOR_CONFIG_FILE = "connector.properties";
    private static final String PEGELHUB_CONFIG_FILE = "pegelhub.yaml";

    /**
     * Initiates the TSTP Connector
     */
    public static void main(String[] args) throws Exception {
        String configDir = resolveConfigDir(args);
        TstpConfigService configParser = new TstpConfigServiceImpl(
                resolveConnectorConfigPath(configDir),
                resolvePegelhubConfigPath(configDir)
        );
        var connector = new TstpConnector(configParser);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutdown issued. Stopping connector...");
            try {
                connector.close();
            } catch (Exception e) {
                LOGGER.error("Couldn't shutdown!");
                throw new RuntimeException(e);
            }
            LOGGER.info("OK");
        }));
    }

    private static String resolveConfigDir(String[] args) {
        return (args.length > 0) ? args[0] : DEFAULT_CONFIG_DIR;
    }

    private static String resolveConnectorConfigPath(String configDir) {
        return Path.of(configDir, CONNECTOR_CONFIG_FILE).toString();
    }

    private static String resolvePegelhubConfigPath(String configDir) {
        return Path.of(configDir, PEGELHUB_CONFIG_FILE).toString();
    }
}
