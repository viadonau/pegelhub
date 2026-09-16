package at.pegelhub.connector.tstp.client;

import at.pegelhub.connector.tstp.codec.TstpXmlCodec;
import at.pegelhub.connector.tstp.codec.TstpBinaryCodec;
import at.pegelhub.connector.tstp.config.TstpServer;
import at.pegelhub.connector.tstp.service.model.XmlTsResponse;
import at.pegelhub.lib.model.Measurement;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.nio.charset.StandardCharsets;

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
    void rejectsMissingOrNonPositiveRequestTimeout() {
        var httpClient = mock(HttpClient.class);
        var codec = mock(TstpXmlCodec.class);

        for (Duration timeout : new Duration[]{null, Duration.ZERO, Duration.ofNanos(-1)}) {
            assertThrows(IllegalArgumentException.class, () ->
                    new HttpTstpClient("localhost", 8030, httpClient, codec, timeout, ZoneOffset.UTC));
        }
    }

    @ParameterizedTest
    @CsvSource({"Z,0", "+01:00,3600", "-03:30,-12600"})
    void transmitsCompleteRawIntervalOverHttpInServerTime(String offset, int seconds) throws Exception {
        AtomicReference<String> query = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            query.set(exchange.getRequestURI().getQuery());
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] confirmation = "<TSR RELEASE=\"1\">confirm</TSR>".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, confirmation.length);
            exchange.getResponseBody().write(confirmation);
            exchange.close();
        });
        server.start();
        TstpXmlCodec codec = new TstpXmlCodec(new TstpBinaryCodec());
        Instant first = Instant.parse("2026-06-07T10:00:00Z");
        try (HttpTstpClient client = HttpTstpClient.open(new TstpServer("127.0.0.1", server.getAddress().getPort(), offset))) {
            client.writeMeasurements("raw-series", List.of(
                    new Measurement(null, first.plusSeconds(600), 3),
                    new Measurement(null, first, 1),
                    new Measurement(null, first.plusSeconds(300), 2)));
            assertEquals("Cmd=PUT&ZRID=raw-series&QUAL=0", query.get());
            List<Measurement> decoded = codec.parseMeasurements(body.get());
            Instant wireStart = first.plusSeconds(seconds);
            assertEquals(List.of(wireStart, wireStart.plusSeconds(300), wireStart.plusSeconds(600)),
                    decoded.stream().map(Measurement::getObservedAt).toList());
            assertEquals(List.of(1.0, 2.0, 3.0), decoded.stream().map(Measurement::getValue).toList());
        } finally {
            server.stop(0);
        }
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void readMeasurementsPadsWireBoundariesWhilePreservingTstpQueryFormat() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        TstpXmlCodec codec = mock(TstpXmlCodec.class);
        HttpResponse<String> httpResponse = mock(HttpResponse.class);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("response");
        when(codec.parseMeasurements("response")).thenReturn(List.of());
        HttpTstpClient client = new HttpTstpClient("localhost", 8030, httpClient, codec,
                Duration.ofSeconds(30), ZoneOffset.UTC);

        client.readMeasurements(
                "PK8n4XrPPUfYpndH6GLH6A",
                Instant.parse("2026-07-19T10:15:30Z"),
                Instant.parse("2026-07-19T11:45:00Z"));

        verify(httpClient).send(
                argThat(request -> request.uri().toString().equals(
                        "http://localhost:8030/?Cmd=Get&ZRID=PK8n4XrPPUfYpndH6GLH6A"
                                + "&Von=2026-07-19T10:15:29Z&Bis=2026-07-19T11:45:01Z&WERTE=True")),
                any(HttpResponse.BodyHandler.class));
    }

    @ParameterizedTest
    @CsvSource({"Z,0", "+01:00,3600", "-03:30,-12600"})
    void filtersInterpolatedEdgesButKeepsARealReadingAtTheLogicalStart(String offset, int seconds) throws Exception {
        AtomicReference<String> query = new AtomicReference<>();
        TstpXmlCodec codec = new TstpXmlCodec(new TstpBinaryCodec());
        Instant start = Instant.parse("2026-09-16T12:00:00Z");
        Instant end = start.plusSeconds(3600);
        byte[] response = codec.writeRequest(List.of(
                new Measurement(null, start.minusSeconds(1), 41.99),
                new Measurement(null, start, 42),
                new Measurement(null, start.plusSeconds(900), 43),
                new Measurement(null, end, 44),
                new Measurement(null, end.plusSeconds(1), 44.01)))
                .getBytes(StandardCharsets.UTF_8);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            query.set(exchange.getRequestURI().getQuery());
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try (HttpTstpClient client = HttpTstpClient.open(new TstpServer("127.0.0.1", server.getAddress().getPort(), offset))) {
            Instant logicalStart = start.minusSeconds(seconds);
            List<Measurement> points = client.readMeasurements("series", logicalStart, end.minusSeconds(seconds));

            assertEquals(List.of(logicalStart, logicalStart.plusSeconds(900)),
                    points.stream().map(Measurement::getObservedAt).toList());
            assertEquals(List.of(42.0, 43.0), points.stream().map(Measurement::getValue).toList());
            assertEquals("Cmd=Get&ZRID=series&Von=2026-09-16T11:59:59Z"
                    + "&Bis=2026-09-16T13:00:01Z&WERTE=True", query.get());
        } finally {
            server.stop(0);
        }
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
        HttpTstpClient client = new HttpTstpClient("localhost", 8030, httpClient, codec,
                Duration.ofSeconds(30), ZoneOffset.UTC);

        assertDoesNotThrow(() -> client.writeMeasurements("zrid", immutable));

        assertEquals(List.of(later, earlier), immutable);
        verify(codec).writeRequest(List.of(earlier, later));
        verify(httpClient).send(
                argThat(request -> request.uri().toString().equals(
                        "http://localhost:8030/?Cmd=PUT&ZRID=zrid&QUAL=0")),
                any(HttpResponse.BodyHandler.class));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void writeMeasurementsRejectsNegativeConfirmationText() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        TstpXmlCodec codec = mock(TstpXmlCodec.class);
        HttpResponse<String> httpResponse = mock(HttpResponse.class);
        XmlTsResponse rejection = new XmlTsResponse();
        rejection.setMessage("not confirmed");

        when(codec.writeRequest(any())).thenReturn("request");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("response");
        when(codec.parseWriteResponse("response")).thenReturn(rejection);
        HttpTstpClient client = new HttpTstpClient("localhost", 8030, httpClient, codec,
                Duration.ofSeconds(30), ZoneOffset.UTC);

        assertThrows(TstpClientException.class, () -> client.writeMeasurements(
                "zrid",
                List.of(new Measurement(null, Instant.parse("2026-06-07T10:00:00Z"), 1.0))));
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
                Duration.ofMillis(100),
                ZoneOffset.UTC);

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
