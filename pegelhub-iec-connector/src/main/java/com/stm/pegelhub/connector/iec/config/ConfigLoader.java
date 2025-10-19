package com.stm.pegelhub.connector.iec.config;

import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Properties;

@Slf4j
public class ConfigLoader {
    private static final String DEFAULT_CONFIG = "/app/files/config.properties";

    public ConnectorOptions parseConfig(String[] args) throws IOException {
        String path = getConfigPath(args);
        Properties props = loadProperties(path);

        log.info("Loaded config from: {}", path);
        props.forEach((key, value) -> log.debug("{} = {}", key, value));

        return new ConnectorOptions(
                props.getProperty("DataPointsDir"),
                getRequiredProperty(props, "Core.IP"),
                Integer.parseInt(getRequiredProperty(props, "Core.Port")),
                InetAddress.getByName(getRequiredProperty(props, "Iec.Host.IP")),
                Integer.parseInt(getRequiredProperty(props, "Iec.Host.Port")),
                Integer.parseInt(getRequiredProperty(props, "Iec.CommonAddress")),
                readDelay(props.getProperty("DelayInterval"))
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

    private String getRequiredProperty(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing or empty property: " + key);
        }
        return value;
    }
}
