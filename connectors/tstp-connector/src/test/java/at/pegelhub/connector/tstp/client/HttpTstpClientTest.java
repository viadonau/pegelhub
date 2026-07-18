package at.pegelhub.connector.tstp.client;

import at.pegelhub.connector.tstp.codec.TstpXmlCodec;
import at.pegelhub.connector.tstp.service.model.XmlTsResponse;
import at.pegelhub.lib.model.Measurement;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpTstpClientTest {
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void readMeasurementsPreservesTstpQueryFormat() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        TstpXmlCodec codec = mock(TstpXmlCodec.class);
        HttpResponse<String> httpResponse = mock(HttpResponse.class);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("response");
        when(codec.parseMeasurements("response")).thenReturn(List.of());
        HttpTstpClient client = new HttpTstpClient("localhost", 8030, httpClient, codec);

        client.readMeasurements(
                "PK8n4XrPPUfYpndH6GLH6A",
                Instant.parse("2026-07-19T10:15:30Z"),
                Instant.parse("2026-07-19T11:45:00Z"));

        verify(httpClient).send(
                argThat(request -> request.uri().toString().equals(
                        "http://localhost:8030/?Cmd=Get&ZRID=PK8n4XrPPUfYpndH6GLH6A"
                                + "&Von=2026-07-19T10:15:30Z&Bis=2026-07-19T11:45:00Z")),
                any(HttpResponse.BodyHandler.class));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void writeMeasurementsSortsACopyOfImmutableInput() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        TstpXmlCodec codec = mock(TstpXmlCodec.class);
        HttpResponse<String> httpResponse = mock(HttpResponse.class);
        Measurement later = new Measurement(null, Instant.parse("2026-06-07T11:00:00Z"), 2.0);
        Measurement earlier = new Measurement(null, Instant.parse("2026-06-07T10:00:00Z"), 1.0);
        List<Measurement> immutable = List.of(later, earlier);
        XmlTsResponse confirmation = new XmlTsResponse();
        confirmation.setMessage("confirm");

        when(codec.writeRequest(List.of(earlier, later))).thenReturn("request");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("response");
        when(codec.parseWriteResponse("response")).thenReturn(confirmation);
        HttpTstpClient client = new HttpTstpClient("localhost", 8030, httpClient, codec);

        assertDoesNotThrow(() -> client.writeMeasurements("zrid", immutable));

        assertEquals(List.of(later, earlier), immutable);
        verify(codec).writeRequest(List.of(earlier, later));
    }

    @Test
    void acceptedButStalledRequestTimesOut() throws Exception {
        CountDownLatch accepted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            accepted.countDown();
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
        HttpTstpClient client = new HttpTstpClient(
                "127.0.0.1",
                server.getAddress().getPort(),
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build(),
                mock(TstpXmlCodec.class),
                Duration.ofMillis(100));

        try {
            TstpClientException error = assertTimeoutPreemptively(
                    Duration.ofSeconds(2),
                    () -> assertThrows(TstpClientException.class, () -> client.readCatalog(1)));

            assertTrue(accepted.await(1, TimeUnit.SECONDS));
            assertTrue(error.getMessage().contains("Query failed"));
        } finally {
            release.countDown();
            client.close();
            server.stop(0);
        }
    }
}
