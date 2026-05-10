package at.pegelhub.connector.tstp.service.impl;

import at.pegelhub.connector.tstp.ConnectorOptions;
import at.pegelhub.connector.tstp.service.TstpConfigService;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Properties;

public class TstpConfigServiceImpl implements TstpConfigService {
    private final String TSTP_CONFIG_PATH;
    private final String CORE_PROPERTIES_PATH;

    public TstpConfigServiceImpl(String tstpConfigPath, String corePropertiesPath) {
        this.TSTP_CONFIG_PATH = tstpConfigPath;
        this.CORE_PROPERTIES_PATH = corePropertiesPath;
    }

    @Override
    public ConnectorOptions getConnectorOptions() throws IOException {
        Properties props = getProperties();
        Duration readDelay = parseDurationString(props.getProperty("connector.readDelay"));

        return new ConnectorOptions(
                props.getProperty("core.address"),
                Integer.parseInt(props.getProperty("core.port")),
                props.getProperty("tstp.address"),
                Integer.parseInt(props.getProperty("tstp.port")),
                readDelay,
                CORE_PROPERTIES_PATH
        );
    }

    @Override
    public Duration parseDurationString(String toParse) {
        if (toParse.isEmpty()) {
            return Duration.ofHours(2);
        }

        String delayDuration = toParse.substring(0, toParse.length() - 1);
        char unit = toParse.charAt(toParse.length() - 1);

        return switch (unit) {
            case 'h', 'H' -> Duration.ofHours(Long.parseLong(delayDuration));
            case 'm', 'M' -> Duration.ofMinutes(Long.parseLong(delayDuration));
            case 's', 'S' -> Duration.ofSeconds(Long.parseLong(delayDuration));
            default -> throw new IllegalArgumentException(String.format("Unknown unit type for time: %c", unit));
        };
    }

    private Properties getProperties() throws IOException {
        Properties props = new Properties();
        props.load(new FileInputStream(TSTP_CONFIG_PATH));
        return props;
    }
}
