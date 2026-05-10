package at.pegelhub.shared.influx;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bean configuration for the influx db connection properties.
 * Configures the conections (url, org, bucket, API-token) for the InfluxDB
 * and the corresponding buckets.
 */
@Configuration
@ConfigurationProperties(prefix = "pegelhub.influx")
@Data
public class InfluxDBConfiguration {
    private static final Logger LOGGER = LogManager.getLogger(InfluxDBConfiguration.class);

    private String url;
    private String org;
    private String token;
    private String dataBucket;
    private String telemetryBucket;

    @Bean("telemetryClient")
    public InfluxDBClient telemetryClient() {
        return createClient(telemetryConfiguration());
    }

    @Bean("dataClient")
    public InfluxDBClient dataClient() {
        return createClient(dataConfiguration());
    }

    @Bean("telemetryConfiguration")
    public DatabaseProperties telemetryConfiguration() {
        return new DatabaseProperties(url, org, telemetryBucket, token);
    }

    @Bean("dataConfiguration")
    public DatabaseProperties dataConfiguration() {
        return new DatabaseProperties(url, org, dataBucket, token);
    }


    private InfluxDBClient createClient(DatabaseProperties properties) {
        LOGGER.trace("creating InfluxDB client for bucket {}", properties.bucket());
        return InfluxDBClientFactory.create(
                properties.url(),
                properties.token().toCharArray(),
                properties.org(),
                properties.bucket());
    }
}
