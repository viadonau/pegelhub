package at.pegelhub.shared.health;

import com.influxdb.client.InfluxDBClient;
import at.pegelhub.shared.influx.InfluxBucketOperations;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration that checks the status of the InfluxDB
 * Needs to be changed if another time series database is going to be used
 */
@Configuration
public class HealthCheckConfig {

    @Bean
    public HealthIndicator influxDbHealthIndicatorMethod(
            @Qualifier("influxDBClient") InfluxDBClient influxDbClient,
            @Qualifier("dataInfluxOperations") InfluxBucketOperations dataInflux,
            @Qualifier("telemetryInfluxOperations") InfluxBucketOperations telemetryInflux) {
        return new InfluxDbHealthIndicator(influxDbClient, List.of(dataInflux, telemetryInflux));
    }
}
