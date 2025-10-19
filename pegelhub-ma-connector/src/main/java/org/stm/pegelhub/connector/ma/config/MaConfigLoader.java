package org.stm.pegelhub.connector.ma.config;

import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Properties;

@Slf4j
public class MaConfigLoader {
    private static final String DEFAULT_CONFIG = "/app/files/config.properties";
    private Properties props;


    /**
     * Parses the connector configuration and builds options.
     *
     * @param args CLI args where index 0 optionally points to the config file
     * @return loaded connector options
     * @throws IOException if the file cannot be read
     */
    public MaConnectorOptions parseConfig(String[] args) throws IOException {
        String path = getConfigPath(args);
        this.props = loadProperties(path);

        log.info("Loaded config from: {}", path);
        props.forEach((key, value) -> log.debug("{} = {}", key, value));

        return new MaConnectorOptions(
                getRequiredProperty("Core.IP"),
                Integer.parseInt(getRequiredProperty("Core.Port")),
                readDelay(getRequiredProperty("DelayInterval")),
                getRequiredProperty("InputsDir")
        );
    }

    private String getConfigPath(String[] args) {
        return (args.length > 0) ? args[0] : DEFAULT_CONFIG;
    }

    private Properties loadProperties(String path) throws IOException {
        Properties props = new Properties();
        try (var stream = new FileInputStream(path)) {
            props.load(stream);
        }
        return props;
    }

    private Duration readDelay(String delay) {
        String number = delay.substring(0, delay.length() - 1);
        char unit = delay.charAt(delay.length() - 1);
        return switch (Character.toLowerCase(unit)) {
            case 'h' -> Duration.ofHours(Long.parseLong(number));
            case 'm' -> Duration.ofMinutes(Long.parseLong(number));
            case 's' -> Duration.ofSeconds(Long.parseLong(number));
            default -> throw new IllegalArgumentException("Unknown unit: " + unit);
        };
    }

    private String getRequiredProperty(String key) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing or empty property: " + key);
        }
        return value;
    }
}
