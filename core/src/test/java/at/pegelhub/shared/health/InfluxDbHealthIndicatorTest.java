package at.pegelhub.shared.health;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.query.FluxTable;
import at.pegelhub.shared.influx.DatabaseProperties;
import at.pegelhub.shared.influx.FluxQueries;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InfluxDbHealthIndicatorTest {

    private static final DatabaseProperties DATA = new DatabaseProperties("url", "org", "data", "token");
    private static final DatabaseProperties TELEMETRY = new DatabaseProperties("url", "org", "telemetry", "token");

    @Test
    void checksAllConfiguredBucketsWhenPingSucceeds() {
        InfluxDBClient client = mock(InfluxDBClient.class);
        QueryApi queryApi = mock(QueryApi.class);
        when(client.ping()).thenReturn(true);
        when(client.getQueryApi()).thenReturn(queryApi);
        when(queryApi.query(FluxQueries.bucketReadCheck(DATA), DATA.org())).thenReturn(List.<FluxTable>of());
        when(queryApi.query(FluxQueries.bucketReadCheck(TELEMETRY), TELEMETRY.org())).thenReturn(List.<FluxTable>of());

        Health health = new InfluxDbHealthIndicator(client, List.of(DATA, TELEMETRY)).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("buckets", List.of("data", "telemetry"));
        verify(queryApi).query(FluxQueries.bucketReadCheck(DATA), DATA.org());
        verify(queryApi).query(FluxQueries.bucketReadCheck(TELEMETRY), TELEMETRY.org());
    }

    @Test
    void reportsDownWhenPingFails() {
        InfluxDBClient client = mock(InfluxDBClient.class);
        when(client.ping()).thenReturn(false);

        Health health = new InfluxDbHealthIndicator(client, List.of(DATA, TELEMETRY)).health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }
}
