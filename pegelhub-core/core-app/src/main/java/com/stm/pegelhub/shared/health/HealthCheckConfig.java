package com.stm.pegelhub.shared.health;

import com.influxdb.client.InfluxDBClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration that checks the status of the InfluxDB
 * Needs to be changed if another time series database is going to be used
 */
@Configuration
public class HealthCheckConfig {

    @Bean
    public HealthIndicator influxDbHealthIndicatorMethod(@Qualifier("dataClient") InfluxDBClient influxDbClient) {
        return new InfluxDbHealthIndicator(influxDbClient);
    }
}
