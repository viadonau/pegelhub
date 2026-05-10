package at.pegelhub.connector.ftp;

import at.pegelhub.connector.ftp.fileparsing.ParserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final String DEFAULT_CONFIG_DIR = "/app/config";
    private static final String CONNECTOR_CONFIG_FILE = "connector.properties";
    private static final String PEGELHUB_CONFIG_FILE = "pegelhub.yaml";


    /**
     * Main method. Initiates the connection to the FTP Server
     */
    public static void main(String[] args) throws Exception {
        var connOpts = getConnectorOptions(resolveConfigDir(args));
        var connector = new FtpConnector(connOpts);
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


    /**
     * Parses the given arguments from the config file to the needed properties to connect to the FTP server
     *
     * @return the parsed ConnectorOptions
     * @throws IOException if an error occurs while reading the properties
     */
    private static ConnectorOptions getConnectorOptions(String configDir) throws IOException {
        Properties props = getProperties(resolveConnectorConfigPath(configDir));

        ParserType parserType = ParserType.valueOfName(props.getProperty("parser.type"));
        if (parserType == null) {
            parserType = ParserType.ASC;
        }

        Duration readDelay = parseReadDelay(props.getProperty("read.delay"));

        return new ConnectorOptions(
                InetAddress.getByName(props.getProperty("core.address")),
                Integer.parseInt(props.getProperty("core.port")),
                InetAddress.getByName(props.getProperty("ftp.address")),
                Integer.parseInt(props.getProperty("ftp.port")),
                props.getProperty("ftp.user"),
                props.getProperty("ftp.password"),
                props.getProperty("ftp.path"),
                parserType,
                readDelay,
                resolvePegelhubConfigPath(configDir)
        );
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

    /**
     * Returns all stored properties from provided property file.
     *
     * @return Properties.
     * @throws IOException If file does not exist.
     */
    private static Properties getProperties(String path) throws IOException {
        Properties props = new Properties();
        try (var stream = new FileInputStream(path)) {
            props.load(stream);
        }
        return props;
    }

    /**
     * Parses the string to a Duration Object if the format is correct
     *
     * @param delayToParse the string to parse
     * @return the parsed Duration
     */
    private static Duration parseReadDelay(String delayToParse) {
        if (delayToParse.isEmpty()) {
            return Duration.ofHours(2);
        }

        String delayDuration = delayToParse.substring(0, delayToParse.length() - 1);
        char unit = delayToParse.charAt(delayToParse.length() - 1);

        return switch (unit) {
            case 'h', 'H' -> Duration.ofHours(Long.parseLong(delayDuration));
            case 'm', 'M' -> Duration.ofMinutes(Long.parseLong(delayDuration));
            case 's', 'S' -> Duration.ofSeconds(Long.parseLong(delayDuration));
            default -> throw new IllegalArgumentException(String.format("Unknown unit type for time: %c", unit));
        };
    }
}
