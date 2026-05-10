package at.pegelhub.testsupport;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;

/**
 * Class for a basic configuration of all tests using InfluxDB.
 */
public abstract class InfluxTestBase {
    protected static final PegelHubInfluxContainer influxContainer = PegelHubInfluxContainer.getInstance();

    static {
        influxContainer.start();
    }

    public static InfluxDBClient getInfluxDBTelemetryClient() {
        return InfluxDBClientFactory.create(
                InfluxDBClientOptions.builder()
                        .url(influxContainer.getUrl())
                        .authenticateToken(PegelHubInfluxContainer.ADMIN_TOKEN.toCharArray())
                        .org(PegelHubInfluxContainer.ORG)
                        .bucket(PegelHubInfluxContainer.TELEMETRY_BUCKET)
                        .build());
    }

    public static InfluxDBClient getInfluxDBDataClient() {
        return InfluxDBClientFactory.create(
                InfluxDBClientOptions.builder()
                        .url(influxContainer.getUrl())
                        .authenticateToken(PegelHubInfluxContainer.ADMIN_TOKEN.toCharArray())
                        .org(PegelHubInfluxContainer.ORG)
                        .bucket(PegelHubInfluxContainer.DATA_BUCKET)
                        .build());
    }
}
