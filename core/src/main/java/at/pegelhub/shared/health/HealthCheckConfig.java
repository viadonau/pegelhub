package at.pegelhub.shared.health;

import com.influxdb.client.InfluxDBClient;
import at.pegelhub.shared.influx.InfluxBucketOperations;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** Configures health checks for the shared InfluxDB client and application buckets. */
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
