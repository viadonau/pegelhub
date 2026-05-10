package com.stm.pegelhub.testsupport;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import org.junit.jupiter.api.BeforeAll;

/**
 * Class for a basic configuration of all tests using InfluxDB.
 */
public abstract class InfluxTestBase {
    protected static PegelHubInfluxContainer influxContainer;


    public static InfluxDBClient getInfluxDBTelemetryClient() {
        return InfluxDBClientFactory.create(
                InfluxDBClientOptions.builder()
                        .url(influxContainer.getUrl())
                        .authenticate("test", "test1234".toCharArray())
                        .org("pegelhub")
                        .bucket("telemetry")
                        .build());
    }

    public static InfluxDBClient getInfluxDBDataClient() {
        return InfluxDBClientFactory.create(
                InfluxDBClientOptions.builder()
                        .url(influxContainer.getUrl())
                        .authenticate("test", "test1234".toCharArray())
                        .org("pegelhub")
                        .bucket("data")
                        .build());
    }

    @BeforeAll
    static void setup() {
        influxContainer = PegelHubInfluxContainer.getInstance();
        influxContainer.start();
    }
}
