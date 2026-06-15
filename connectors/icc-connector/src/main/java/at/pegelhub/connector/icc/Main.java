package at.pegelhub.connector.icc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;


/**
 * Entry point of the ICC Connector.
 * Reads the 2 endpoints from the config file + interval in which data for selected suppliers is fetched from
 * supplying PH (source) to receiving PH (sink).
 */
public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static final String DEFAULT_CONFIG_DIR = "/app/config";
    private static final String CONNECTOR_CONFIG_FILE = "connector.properties";
    private static final String SOURCE_PEGELHUB_FILE = "source-pegelhub.yaml";
    private static final String SINK_PEGELHUB_FILE = "sink-pegelhub.yaml";
    private static IccConnector _icc;

    private static Duration readDelay(String delay) {
            String number = delay.substring(0, delay.length() - 1);
            char unit = delay.charAt(delay.length() - 1);
            return switch (unit) {
                case 'h', 'H' -> Duration.ofHours(Long.parseLong(number));
                case 'm', 'M' -> Duration.ofMinutes(Long.parseLong(number));
                case 's', 'S' -> Duration.ofSeconds(Long.parseLong(number));
                default -> throw new IllegalArgumentException(String.format("Unknown unit type for time: %c", unit));
            };
    }

    public static void main(String[] args) throws Exception {
        String configDir = getConfigDir(args);

        Properties props = getProperties(resolveConnectorConfigPath(configDir));

        String sourceUrl = (String)props.get("Core.Source");
        String sinkUrl = (String)props.get("Core.Sink");
        String sourceTimeSeriesIds = (String) props.get("Icc.SourceTimeSeriesId");
        String refreshInterval = (String)props.get("Icc.RefreshInterval");
        Duration delay = readDelay(refreshInterval);

        LOG.info("SourceUrl: {}", sourceUrl);
        LOG.info("SinkUrl: {}", sinkUrl);
        LOG.info("SourceTimeSeriesIds: {}", sourceTimeSeriesIds);
        LOG.info("Interval: {}", delay);

        List<UUID> sourceTimeSeriesIdList = parseTimeSeriesIds(sourceTimeSeriesIds);

        try {
            _icc = new IccConnector(
                    URI.create(sourceUrl).toURL(), resolveSourcePegelhubPath(configDir),
                    URI.create(sinkUrl).toURL(), resolveSinkPegelhubPath(configDir),
                    sourceTimeSeriesIdList, delay, refreshInterval
            );
        } catch (Exception ex) {
            LOG.error("Creation of ICC Connector failed");
        }

        if (_icc != null) {
            // Graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOG.info("Shutdown issued. Stopping connector...");
                try {
                    _icc.close();
                } catch (Exception e) {
                    LOG.error("Couldn't shutdown!");
                    throw new RuntimeException(e);
                }
                LOG.info("OK");
            }));
        }
    }

    /**
     * Returns all stored properties from provided property file.
     * @param path Path to the property file.
     * @return Properties.
     * @throws IOException If file does not exist.
     */
    private static Properties getProperties(String path) throws IOException {
        Properties props = new Properties();
        props.load(new FileInputStream(path));
        return props;
    }

    /**
     * Returns path to config file from provided command line args or the fallback path.
     * @param args Command line arguments.
     * @return Path to the config directory.
     */
    private static String getConfigDir(String[] args) {
        if (args.length > 0) {
            LOG.info("Using config dir {}", args[0]);
        }
        return (args.length > 0) ? args[0] : DEFAULT_CONFIG_DIR;
    }

    private static String resolveConnectorConfigPath(String configDir) {
        return Path.of(configDir, CONNECTOR_CONFIG_FILE).toString();
    }

    private static String resolveSourcePegelhubPath(String configDir) {
        return Path.of(configDir, SOURCE_PEGELHUB_FILE).toString();
    }

    private static String resolveSinkPegelhubPath(String configDir) {
        return Path.of(configDir, SINK_PEGELHUB_FILE).toString();
    }

    private static List<UUID> parseTimeSeriesIds(String sourceTimeSeriesIds) {
        return Arrays.stream(sourceTimeSeriesIds.split(","))
                .map(String::trim)
                .filter(id -> !id.isBlank())
                .map(UUID::fromString)
                .toList();
    }
}
