package at.pegelhub.shared.influx;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import static java.util.Objects.requireNonNull;

/**
 * Bean configuration for the influx db connection properties.
 * Configures the connection and the corresponding buckets.
 */
@Configuration
@EnableConfigurationProperties(InfluxProperties.class)
public class InfluxDBConfiguration {
    private static final Logger LOGGER = LogManager.getLogger(InfluxDBConfiguration.class);

    @Bean(name = {"influxDBClient", "dataClient", "telemetryClient"}, destroyMethod = "close")
    public InfluxDBClient influxDBClient(InfluxProperties properties) {
        requireNonNull(properties);
        LOGGER.trace("creating shared InfluxDB client for {}", properties.url());
        return InfluxDBClientFactory.create(
                properties.url(),
                properties.token().toCharArray(),
                properties.org());
    }

    @Bean("dataConfiguration")
    public DatabaseProperties dataConfiguration(InfluxProperties properties) {
        requireNonNull(properties);
        return properties.dataDatabase();
    }

    @Bean("telemetryConfiguration")
    public DatabaseProperties telemetryConfiguration(InfluxProperties properties) {
        requireNonNull(properties);
        return properties.telemetryDatabase();
    }

    @Bean("latestRange")
    public FluxDuration latestRange(InfluxProperties properties) {
        requireNonNull(properties);
        return properties.latestRangeDuration();
    }
}
