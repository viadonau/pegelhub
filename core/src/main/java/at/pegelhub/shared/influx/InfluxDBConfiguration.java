package at.pegelhub.shared.influx;

import at.pegelhub.shared.duration.PegelhubDurationLiteral;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
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

    @Bean("dataInfluxOperations")
    public InfluxBucketOperations dataInfluxOperations(
            @Qualifier("influxDBClient") InfluxDBClient client,
            @Qualifier("dataConfiguration") DatabaseProperties database) {
        return new InfluxBucketOperations(client, database);
    }

    @Bean("telemetryInfluxOperations")
    public InfluxBucketOperations telemetryInfluxOperations(
            @Qualifier("influxDBClient") InfluxDBClient client,
            @Qualifier("telemetryConfiguration") DatabaseProperties database) {
        return new InfluxBucketOperations(client, database);
    }

    @Bean("latestRange")
    public PegelhubDurationLiteral latestRange(InfluxProperties properties) {
        requireNonNull(properties);
        return properties.latestRangeDuration();
    }
}
