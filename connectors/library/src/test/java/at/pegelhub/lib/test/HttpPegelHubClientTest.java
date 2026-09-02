package at.pegelhub.lib.test;

import at.pegelhub.lib.config.CoreAuthentication;
import at.pegelhub.lib.internal.HttpPegelHubClient;
import at.pegelhub.lib.model.Measurement;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.jupiter.api.*;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class HttpPegelHubClientTest {
    private static final Instant READ_FROM = Instant.parse("2026-06-16T00:00:00Z");
    private static final Instant READ_TO = Instant.parse("2026-06-17T00:00:00Z");

    public CloseableHttpClient httpClient;
    public CoreAuthentication authentication;
    public HttpPegelHubClient phc;
    public UUID uuid;

    @BeforeEach
    public void setup() throws MalformedURLException {
        httpClient = mock(CloseableHttpClient.class);
        authentication = new CoreAuthentication(
                "http://keycloak.local/token", "local-connector-example", "secret");
        phc = new HttpPegelHubClient(httpClient, baseUrl(), authentication);
        uuid = UUID.fromString("74bcffac-8fa6-41ac-aa9d-53d082447226");
    }

    @Test
    public void httpClientIsClosedOnCloseAndDoesNotThrow() {
        assertDoesNotThrow(() -> {
            phc.close();
            verify(httpClient, times(1)).close();
        });
    }

    @Test
    public void requestsUseBearerTokenWhenOAuthConfigIsPresent() throws IOException {
        List<String> requestUris = new ArrayList<>();
        List<String> authorizationHeaders = new ArrayList<>();
        when(httpClient.execute(any(), any(HttpClientResponseHandler.class))).thenAnswer(a -> {
            var request = (org.apache.hc.client5.http.classic.methods.HttpUriRequestBase) a.getRawArguments()[0];
            requestUris.add(request.getUri().toString());
            var authorization = request.getFirstHeader("Authorization");
            authorizationHeaders.add(authorization == null ? null : authorization.getValue());

            var responseCallback = (HttpClientResponseHandler<?>) a.getRawArguments()[1];
            ClassicHttpResponse httpResp = mock(ClassicHttpResponse.class);
            HttpEntity entity = mock(HttpEntity.class);
            String body = request.getUri().toString().contains("keycloak.local")
                    ? "{\"access_token\":\"local-access-token\",\"expires_in\":300}"
                    : getResource("MeasurementsEmptyResponse.json");
            when(entity.getContent()).thenReturn(new ByteArrayInputStream(body.getBytes()));
            when(httpResp.getEntity()).thenReturn(entity);
            when(httpResp.getCode()).thenReturn(HttpStatus.SC_OK);
            return responseCallback.handleResponse(httpResp);
        });

        phc.getMeasurementsOfTimeSeries(
                UUID.fromString("395c0232-d110-40fd-bd7f-2bb4a0f2009d"), READ_FROM, READ_TO);

        assertEquals("http://keycloak.local/token", requestUris.get(0));
        assertEquals("Bearer local-access-token", authorizationHeaders.get(1));
        assertFalse(requestUris.get(1).contains("apiKey"));
    }

    @Test
    public void generatedCoreUrlsDoNotContainApiKey() throws IOException {
        List<String> requestUris = new ArrayList<>();
        when(httpClient.execute(any(), any(HttpClientResponseHandler.class))).thenAnswer(a -> {
            var request = (org.apache.hc.client5.http.classic.methods.HttpUriRequestBase) a.getRawArguments()[0];
            requestUris.add(request.getUri().toString());
            var responseCallback = (HttpClientResponseHandler<?>) a.getRawArguments()[1];
            ClassicHttpResponse httpResp = mock(ClassicHttpResponse.class);
            HttpEntity entity = mock(HttpEntity.class);
            String body = request.getUri().toString().contains("keycloak.local")
                    ? "{\"access_token\":\"local-access-token\",\"expires_in\":300}"
                    : getResource("MeasurementsEmptyResponse.json");
            when(entity.getContent()).thenReturn(new ByteArrayInputStream(body.getBytes()));
            when(httpResp.getEntity()).thenReturn(entity);
            when(httpResp.getCode()).thenReturn(HttpStatus.SC_OK);
            return responseCallback.handleResponse(httpResp);
        });

        phc.getMeasurementsOfTimeSeries(
                UUID.fromString("395c0232-d110-40fd-bd7f-2bb4a0f2009d"), READ_FROM, READ_TO);

        assertFalse(requestUris.getFirst().contains("apiKey"));
    }

    @Test
    public void constructorSkipsStartupMetadata() throws MalformedURLException {
        var startupAuthentication = new CoreAuthentication(
                "http://keycloak.local/token", "local-connector-example", "secret");

        new HttpPegelHubClient(httpClient, baseUrl(), startupAuthentication);

        verifyNoMoreInteractions(httpClient);
    }

    @Test
    public void coreAuthenticationFailsFastWhenOAuthConfigurationIsMissing() {
        assertThrows(IllegalArgumentException.class, () ->
                new CoreAuthentication(null, "local-connector-example", "secret"));
    }

    @Nested
    @DisplayName("Measurement API Tests")
    class MeasurementClientTest {
        @Test
        public void getMeasurementsOfTimeSeries_UsesTimeSeriesRoute() throws IOException {
            List<String> requestUris = new ArrayList<>();
            when(httpClient.execute(any(), any(HttpClientResponseHandler.class))).thenAnswer(a -> {
                var request = (org.apache.hc.client5.http.classic.methods.HttpUriRequestBase) a.getRawArguments()[0];
                requestUris.add(request.getUri().toString());
                var responseCallback = (HttpClientResponseHandler<?>) a.getRawArguments()[1];
                ClassicHttpResponse httpResp = mock(ClassicHttpResponse.class);
                HttpEntity entity = mock(HttpEntity.class);
                String body = request.getUri().toString().contains("keycloak.local")
                        ? "{\"access_token\":\"local-access-token\",\"expires_in\":300}"
                        : getResource("MeasurementsFilledResponse.json");
                when(entity.getContent()).thenReturn(new ByteArrayInputStream(body.getBytes()));
                when(httpResp.getEntity()).thenReturn(entity);
                when(httpResp.getCode()).thenReturn(HttpStatus.SC_OK);
                return responseCallback.handleResponse(httpResp);
            });

            UUID timeSeriesId = UUID.fromString("395c0232-d110-40fd-bd7f-2bb4a0f2009d");

            Collection<Measurement> measurements = phc.getMeasurementsOfTimeSeries(timeSeriesId, READ_FROM, READ_TO);

            assertFalse(measurements.isEmpty());
            assertEquals(
                    "http://localhost:1111/api/v1/time-series/395c0232-d110-40fd-bd7f-2bb4a0f2009d/measurements"
                            + "?from=2026-06-16T00%3A00%3A00Z&to=2026-06-17T00%3A00%3A00Z&order=asc&limit=10000",
                    requestUris.get(1));
        }

        @Test
        public void getMeasurementsOfTimeSeries_MapsCurrentCoreMeasurementEnvelope() throws IOException {
            mockSuccessfulResponse(getResource("CoreMeasurementListResponse.json"));

            UUID timeSeriesId = UUID.fromString("395c0232-d110-40fd-bd7f-2bb4a0f2009d");
            Collection<Measurement> measurements =
                    phc.getMeasurementsOfTimeSeries(timeSeriesId, READ_FROM, READ_TO);

            assertEquals(1, measurements.size());
            Measurement measurement = measurements.iterator().next();
            assertEquals(UUID.fromString("395c0232-d110-40fd-bd7f-2bb4a0f2009d"), measurement.getTimeSeriesId());
            assertEquals(Instant.parse("2026-06-17T12:00:00Z"), measurement.getObservedAt());
            assertEquals(2.73, measurement.getValue());
        }

        @Test
        void getMeasurementsBisectsTruncatedWindows() throws IOException {
            Instant middle = READ_FROM.plus(Duration.between(READ_FROM, READ_TO).dividedBy(2));
            Instant firstObservedAt = middle.minusSeconds(1);
            Instant secondObservedAt = middle.plusSeconds(1);
            var pages = new ArrayDeque<>(List.of(
                    measurementListResponse(uuid, true, middle, 0.0),
                    measurementListResponse(uuid, false, firstObservedAt, 1.0),
                    measurementListResponse(uuid, false, secondObservedAt, 2.0)));
            List<String> requestUris = new ArrayList<>();
            when(httpClient.execute(any(), any(HttpClientResponseHandler.class))).thenAnswer(answer -> {
                var request = (org.apache.hc.client5.http.classic.methods.HttpUriRequestBase)
                        answer.getRawArguments()[0];
                var responseCallback = (HttpClientResponseHandler<?>) answer.getRawArguments()[1];
                boolean tokenRequest = request.getUri().toString().contains("keycloak.local");
                ClassicHttpResponse response = mock(ClassicHttpResponse.class);
                HttpEntity entity = mock(HttpEntity.class);
                String body = tokenRequest
                        ? "{\"access_token\":\"local-access-token\",\"expires_in\":300}"
                        : pages.removeFirst();
                if (!tokenRequest) {
                    requestUris.add(request.getUri().toString());
                }
                when(entity.getContent()).thenReturn(new ByteArrayInputStream(body.getBytes()));
                when(response.getEntity()).thenReturn(entity);
                when(response.getCode()).thenReturn(HttpStatus.SC_OK);
                return responseCallback.handleResponse(response);
            });

            Collection<Measurement> measurements =
                    phc.getMeasurementsOfTimeSeries(uuid, READ_FROM, READ_TO);

            assertEquals(List.of(firstObservedAt, secondObservedAt), measurements.stream()
                    .map(Measurement::getObservedAt)
                    .toList());
            assertTrue(pages.isEmpty());
            assertEquals(3, requestUris.size());
            assertTrue(requestUris.get(1).contains("to=2026-06-16T12%3A00%3A00Z"));
            assertTrue(requestUris.get(2).contains("from=2026-06-16T12%3A00%3A00Z"));
        }

        @Test
        void getMeasurementsPreservesMidpointAndSharedTimestampsWhenBisecting() throws IOException {
            Instant middle = READ_FROM.plus(Duration.between(READ_FROM, READ_TO).dividedBy(2));
            Instant beforeMiddle = middle.minusNanos(1);
            Instant afterMiddle = middle.plusNanos(1);
            var pages = new ArrayDeque<>(List.of(
                    measurementListResponse(uuid, true, middle, 0.0),
                    measurementListResponse(uuid, false, beforeMiddle, 1.0),
                    measurementListResponse(uuid, false, List.of(
                            new Measurement(uuid, middle, 2.0),
                            new Measurement(uuid, middle, 3.0),
                            new Measurement(uuid, afterMiddle, 4.0)))));
            List<String> requestUris = new ArrayList<>();
            when(httpClient.execute(any(), any(HttpClientResponseHandler.class))).thenAnswer(answer -> {
                var request = (org.apache.hc.client5.http.classic.methods.HttpUriRequestBase)
                        answer.getRawArguments()[0];
                var responseCallback = (HttpClientResponseHandler<?>) answer.getRawArguments()[1];
                boolean tokenRequest = request.getUri().toString().contains("keycloak.local");
                ClassicHttpResponse response = mock(ClassicHttpResponse.class);
                HttpEntity entity = mock(HttpEntity.class);
                String body = tokenRequest
                        ? "{\"access_token\":\"local-access-token\",\"expires_in\":300}"
                        : pages.removeFirst();
                if (!tokenRequest) {
                    requestUris.add(request.getUri().toString());
                }
                when(entity.getContent()).thenReturn(new ByteArrayInputStream(body.getBytes()));
                when(response.getEntity()).thenReturn(entity);
                when(response.getCode()).thenReturn(HttpStatus.SC_OK);
                return responseCallback.handleResponse(response);
            });

            List<Measurement> measurements = List.copyOf(
                    phc.getMeasurementsOfTimeSeries(uuid, READ_FROM, READ_TO));

            assertEquals(List.of(beforeMiddle, middle, middle, afterMiddle), measurements.stream()
                    .map(Measurement::getObservedAt)
                    .toList());
            assertEquals(List.of(1.0, 2.0, 3.0, 4.0), measurements.stream()
                    .map(Measurement::getValue)
                    .toList());
            assertTrue(pages.isEmpty());
            assertEquals(3, requestUris.size());
            assertTrue(requestUris.get(1).contains("to=2026-06-16T12%3A00%3A00Z"));
            assertTrue(requestUris.get(2).contains("from=2026-06-16T12%3A00%3A00Z"));
        }

        @Test
        void getMeasurementsRejectsTruncatedIndivisibleWindow() throws IOException {
            mockSuccessfulResponse(measurementListResponse(
                    uuid,
                    true,
                    READ_FROM,
                    2.73));

            RuntimeException error = assertThrows(RuntimeException.class,
                    () -> phc.getMeasurementsOfTimeSeries(uuid, READ_FROM, READ_FROM.plusNanos(1)));

            assertInstanceOf(IllegalStateException.class, error.getCause());
            assertTrue(error.getCause().getMessage().contains("indivisible"));
        }

        @Test
        void getMeasurementsRejectsMismatchedTimeSeries() throws IOException {
            mockSuccessfulResponse(getResource("CoreMeasurementListResponse.json"));

            RuntimeException error = assertThrows(RuntimeException.class,
                    () -> phc.getMeasurementsOfTimeSeries(uuid, READ_FROM, READ_TO));

            assertInstanceOf(IllegalStateException.class, error.getCause());
            assertTrue(error.getCause().getMessage().contains(uuid.toString()));
        }

        @Test
        void getLatestMeasurementAcceptsTruncationBecauseItRequestsOnlyOneValue() throws IOException {
            mockSuccessfulResponse(getResource("CoreMeasurementListTruncatedResponse.json"));

            Optional<Measurement> measurement = phc.getLatestMeasurementOfTimeSeries(uuid);

            assertTrue(measurement.isPresent());
            assertEquals(uuid, measurement.orElseThrow().getTimeSeriesId());
        }

        @Test
        public void getLatestMeasurementOfTimeSeries_UsesTimeSeriesRoute() throws IOException {
            List<String> requestUris = new ArrayList<>();
            when(httpClient.execute(any(), any(HttpClientResponseHandler.class))).thenAnswer(a -> {
                var request = (org.apache.hc.client5.http.classic.methods.HttpUriRequestBase) a.getRawArguments()[0];
                requestUris.add(request.getUri().toString());
                var responseCallback = (HttpClientResponseHandler<?>) a.getRawArguments()[1];
                ClassicHttpResponse httpResp = mock(ClassicHttpResponse.class);
                HttpEntity entity = mock(HttpEntity.class);
                String body = request.getUri().toString().contains("keycloak.local")
                        ? "{\"access_token\":\"local-access-token\",\"expires_in\":300}"
                        : getResource("Measurement.json");
                when(entity.getContent()).thenReturn(new ByteArrayInputStream(body.getBytes()));
                when(httpResp.getEntity()).thenReturn(entity);
                when(httpResp.getCode()).thenReturn(HttpStatus.SC_OK);
                return responseCallback.handleResponse(httpResp);
            });

            UUID timeSeriesId = UUID.fromString("395c0232-d110-40fd-bd7f-2bb4a0f2009d");

            Optional<Measurement> measurement = phc.getLatestMeasurementOfTimeSeries(timeSeriesId);

            assertTrue(measurement.isPresent());
            assertEquals(
                    "http://localhost:1111/api/v1/time-series/395c0232-d110-40fd-bd7f-2bb4a0f2009d/measurements?last=365d&order=desc&limit=1",
                    requestUris.get(1));
        }

        @Test
        void getMeasurementsThrowsWhenCoreRejectsTheRequest() throws IOException {
            mockTokenSuccessAndCoreResponse(HttpStatus.SC_UNAUTHORIZED, "unauthorized");

            RuntimeException error = assertThrows(RuntimeException.class,
                    () -> phc.getMeasurementsOfTimeSeries(uuid, READ_FROM, READ_TO));

            assertNotNull(error.getCause());
            assertTrue(error.getCause().getMessage().contains("401"));
        }

        @Test
        void getLatestMeasurementThrowsWhenCoreFails() throws IOException {
            mockTokenSuccessAndCoreResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "failure");

            RuntimeException error = assertThrows(RuntimeException.class,
                    () -> phc.getLatestMeasurementOfTimeSeries(uuid));

            assertNotNull(error.getCause());
            assertTrue(error.getCause().getMessage().contains("500"));
        }

        @Test
        void getLatestMeasurementDistinguishesMissingTimeSeries() throws IOException {
            mockTokenSuccessAndCoreResponse(HttpStatus.SC_NOT_FOUND, "missing");

            assertThrows(at.pegelhub.lib.exception.NotFoundException.class,
                    () -> phc.getLatestMeasurementOfTimeSeries(uuid));
        }

        @Test
        public void sendMeasurements_DoesNotThrowWhenHandlingOKResponse() throws IOException {
            mockSuccessfulResponse(getResource("EmptyResponse.json"));

            assertDoesNotThrow(() -> {
                var meas = measurement();
                phc.sendMeasurements(List.of(meas));
            });
        }

        @Test
        public void sendMeasurements_SerializesCleanMeasurementAsUtcJson() throws IOException {
            List<String> requestBodies = new ArrayList<>();
            when(httpClient.execute(any(), any(HttpClientResponseHandler.class))).thenAnswer(a -> {
                var request = (org.apache.hc.client5.http.classic.methods.HttpUriRequestBase) a.getRawArguments()[0];
                var responseCallback = (HttpClientResponseHandler<?>) a.getRawArguments()[1];
                ClassicHttpResponse httpResp = mock(ClassicHttpResponse.class);
                HttpEntity entity = mock(HttpEntity.class);
                String body = request.getUri().toString().contains("keycloak.local")
                        ? "{\"access_token\":\"local-access-token\",\"expires_in\":300}"
                        : "";
                if (!request.getUri().toString().contains("keycloak.local")) {
                    requestBodies.add(EntityUtils.toString(request.getEntity()));
                }
                when(entity.getContent()).thenReturn(new ByteArrayInputStream(body.getBytes()));
                when(httpResp.getEntity()).thenReturn(entity);
                when(httpResp.getCode()).thenReturn(HttpStatus.SC_OK);
                return responseCallback.handleResponse(httpResp);
            });

            phc.sendMeasurements(List.of(new Measurement(
                    UUID.fromString("395c0232-d110-40fd-bd7f-2bb4a0f2009d"),
                    Instant.parse("2026-04-25T10:15:30Z"),
                    1.0)));

            assertEquals(1, requestBodies.size());
            assertTrue(requestBodies.getFirst().contains("\"timeSeriesId\":\"395c0232-d110-40fd-bd7f-2bb4a0f2009d\""));
            assertTrue(requestBodies.getFirst().contains("\"observedAt\":\"2026-04-25T10:15:30Z\""));
            assertTrue(requestBodies.getFirst().contains("\"value\":1.0"));
        }

        @Test
        public void sendMeasurements_ThrowsWhenHandlingBadResponse() throws IOException {
            mockFailedResponse(400);

            assertThrows(Exception.class, () -> {
                var meas = measurement();
                phc.sendMeasurements(List.of(meas));
            });
        }

        @Test
        public void sendMeasurements_ThrowsWhenMeasurementIsMissingTimeSeriesId() {
            assertThrows(RuntimeException.class, () -> phc.sendMeasurements(List.of(new Measurement())));
        }

        private Measurement measurement() {
            return new Measurement(
                    UUID.fromString("395c0232-d110-40fd-bd7f-2bb4a0f2009d"),
                    Instant.parse("2026-04-25T10:15:30Z"),
                    1.0);
        }
    }

    private List<String> mockSuccessfulResponse(String response) throws IOException {
        List<String> requestUris = new ArrayList<>();
        when(httpClient.execute(any(), any(HttpClientResponseHandler.class))).thenAnswer(a -> {
            var request = (org.apache.hc.client5.http.classic.methods.HttpUriRequestBase) a.getRawArguments()[0];
            requestUris.add(request.getUri().toString());
            var responseCallback = (HttpClientResponseHandler<?>) a.getRawArguments()[1];
            ClassicHttpResponse httpResp = mock(ClassicHttpResponse.class);
            HttpEntity entity = mock(HttpEntity.class);
            String body = request.getUri().toString().contains("keycloak.local")
                    ? "{\"access_token\":\"local-access-token\",\"expires_in\":300}"
                    : response;
            when(entity.getContent()).thenReturn(new ByteArrayInputStream(body.getBytes()));
            when(httpResp.getEntity()).thenReturn(entity);
            when(httpResp.getCode()).thenReturn(HttpStatus.SC_OK);
            return responseCallback.handleResponse(httpResp);
        });
        return requestUris;
    }

    private void mockFailedResponse(int code) throws IOException {
        when(httpClient.execute(any(), any(HttpClientResponseHandler.class))).thenAnswer(a -> {
            var responseCallback = (HttpClientResponseHandler<?>) a.getRawArguments()[1];
            ClassicHttpResponse httpResp = mock(ClassicHttpResponse.class);
            HttpEntity entity = mock(HttpEntity.class);
            when(entity.getContent()).thenReturn(new ByteArrayInputStream(new byte[0]));
            when(httpResp.getEntity()).thenReturn(entity);
            when(httpResp.getCode()).thenReturn(code);
            return responseCallback.handleResponse(httpResp);
        });
    }

    private void mockTokenSuccessAndCoreResponse(int coreStatus, String coreBody) throws IOException {
        when(httpClient.execute(any(), any(HttpClientResponseHandler.class))).thenAnswer(a -> {
            var request = (org.apache.hc.client5.http.classic.methods.HttpUriRequestBase) a.getRawArguments()[0];
            var responseCallback = (HttpClientResponseHandler<?>) a.getRawArguments()[1];
            boolean tokenRequest = request.getUri().toString().contains("keycloak.local");
            ClassicHttpResponse response = mock(ClassicHttpResponse.class);
            HttpEntity entity = mock(HttpEntity.class);
            String body = tokenRequest
                    ? "{\"access_token\":\"local-access-token\",\"expires_in\":300}"
                    : coreBody;
            when(entity.getContent()).thenReturn(new ByteArrayInputStream(body.getBytes()));
            when(response.getEntity()).thenReturn(entity);
            when(response.getCode()).thenReturn(tokenRequest ? HttpStatus.SC_OK : coreStatus);
            return responseCallback.handleResponse(response);
        });
    }

    private URL baseUrl() throws MalformedURLException {
        return URI.create("http://localhost:1111/").toURL();
    }

    private String measurementListResponse(
            UUID timeSeriesId,
            boolean truncated,
            Instant observedAt,
            double value) {
        return measurementListResponse(
                timeSeriesId,
                truncated,
                List.of(new Measurement(timeSeriesId, observedAt, value)));
    }

    private String measurementListResponse(
            UUID timeSeriesId,
            boolean truncated,
            List<Measurement> measurements) {
        String values = measurements.stream()
                .map(measurement -> """
                        {"observedAt": "%s", "value": %s}
                        """.formatted(measurement.getObservedAt(), measurement.getValue()).strip())
                .collect(Collectors.joining(",\n"));
        return """
                {
                  "timeSeriesId": "%s",
                  "truncated": %s,
                  "measurements": [
                    %s
                  ]
                }
                """.formatted(timeSeriesId, truncated, values);
    }

    private String getResource(String name) throws IOException {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        try (InputStream is = cl.getResourceAsStream(name)) {
            if (is == null) {
                return "";
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                return br.lines().collect(Collectors.joining());
            }
        }
    }
}
