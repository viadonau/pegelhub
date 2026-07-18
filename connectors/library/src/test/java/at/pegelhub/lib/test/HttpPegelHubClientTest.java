package at.pegelhub.lib.test;

import at.pegelhub.lib.ClientCredentials;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class HttpPegelHubClientTest {
    public CloseableHttpClient httpClient;
    public ClientCredentials credentials;
    public HttpPegelHubClient phc;
    public UUID uuid;

    @BeforeEach
    public void setup() throws MalformedURLException {
        httpClient = mock(CloseableHttpClient.class);
        credentials = new ClientCredentials("http://keycloak.local/token", "local-connector-example", "secret");
        phc = new HttpPegelHubClient(httpClient, baseUrl(), credentials);
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

        phc.getMeasurementsOfTimeSeries(uuid, "72h");

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

        phc.getMeasurementsOfTimeSeries(uuid, "72h");

        assertFalse(requestUris.getFirst().contains("apiKey"));
    }

    @Test
    public void constructorSkipsStartupMetadata() throws MalformedURLException {
        var startupCredentials = new ClientCredentials("http://keycloak.local/token", "local-connector-example", "secret");

        new HttpPegelHubClient(httpClient, baseUrl(), startupCredentials);

        verifyNoMoreInteractions(httpClient);
    }

    @Test
    public void clientCredentialsFailFastWhenOAuthConfigurationIsMissing() {
        assertThrows(IllegalArgumentException.class, () ->
                new ClientCredentials(null, "local-connector-example", "secret"));
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

            Collection<Measurement> measurements = phc.getMeasurementsOfTimeSeries(timeSeriesId, "72h");

            assertFalse(measurements.isEmpty());
            assertEquals(
                    "http://localhost:1111/api/v1/time-series/395c0232-d110-40fd-bd7f-2bb4a0f2009d/measurements?last=72h",
                    requestUris.get(1));
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
                    () -> phc.getMeasurementsOfTimeSeries(uuid, "72h"));

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
