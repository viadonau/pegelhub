package at.pegelhub.shared.influx;

import com.influxdb.client.InfluxDBClient;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

class InfluxDeadlineTest {
    @Test
    void totalCallTimeoutCancelsAStalledServerWithoutChangingSharedClient() throws Exception {
        try (var server = new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
            var thread = Thread.ofPlatform().start(() -> {
                try (var socket = server.accept()) {
                    socket.getInputStream().read();
                    Thread.sleep(600);
                } catch (Exception ignored) {
                    Thread.currentThread().interrupt();
                }
            });

            try {
                var operations = new InfluxBucketOperations(mock(InfluxDBClient.class),
                        new DatabaseProperties("http://localhost:" + server.getLocalPort(), "org", "bucket", "test-token"));
                long started = System.nanoTime();

                assertThatThrownBy(() -> operations.query("from(bucket: \"bucket\") |> range(start: -1h)", Duration.ofMillis(100)))
                        .isInstanceOf(RuntimeException.class);
                assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(2));
            } finally {
                thread.join(2000);
            }
        }
    }
}
