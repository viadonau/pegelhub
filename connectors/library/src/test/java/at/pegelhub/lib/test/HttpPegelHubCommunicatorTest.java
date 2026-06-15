package at.pegelhub.lib.test;

import at.pegelhub.lib.internal.ApplicationProperties;
import at.pegelhub.lib.internal.HttpPegelHubCommunicator;
import at.pegelhub.lib.model.Connector;
import at.pegelhub.lib.model.Contact;
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

public class HttpPegelHubCommunicatorTest {
    public CloseableHttpClient httpClient;
    public ApplicationProperties properties;
    public HttpPegelHubCommunicator phc;
    public UUID uuid;

    @BeforeEach
    public void setup() throws MalformedURLException {
        httpClient = mock(CloseableHttpClient.class);
        properties = mock(ApplicationProperties.class);
        when(properties.getTokenUrl()).thenReturn("http://keycloak.local/token");
        when(properties.getClientId()).thenReturn("local-connector-example");
        when(properties.getClientSecret()).thenReturn("secret");
        phc = new HttpPegelHubCommunicator(httpClient, baseUrl(), properties);
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
        when(properties.getTokenUrl()).thenReturn("http://keycloak.local/token");
        when(properties.getClientId()).thenReturn("local-connector-example");
        when(properties.getClientSecret()).thenReturn("secret");
        when(properties.isSupplier()).thenReturn(false);

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
                    : getResource("EmptyArray.json");
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
        when(properties.isSupplier()).thenReturn(false);
        List<String> requestUris = new ArrayList<>();
        when(httpClient.execute(any(), any(HttpClientResponseHandler.class))).thenAnswer(a -> {
            var request = (org.apache.hc.client5.http.classic.methods.HttpUriRequestBase) a.getRawArguments()[0];
            requestUris.add(request.getUri().toString());
            var responseCallback = (HttpClientResponseHandler<?>) a.getRawArguments()[1];
            ClassicHttpResponse httpResp = mock(ClassicHttpResponse.class);
            HttpEntity entity = mock(HttpEntity.class);
            String body = request.getUri().toString().contains("keycloak.local")
                    ? "{\"access_token\":\"local-access-token\",\"expires_in\":300}"
                    : getResource("EmptyArray.json");
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
        var startupProperties = mock(ApplicationProperties.class);
        when(startupProperties.getTokenUrl()).thenReturn("http://keycloak.local/token");
        when(startupProperties.getClientId()).thenReturn("local-connector-example");
        when(startupProperties.getClientSecret()).thenReturn("secret");

        new HttpPegelHubCommunicator(httpClient, baseUrl(), startupProperties);

        verifyNoMoreInteractions(httpClient);
    }

    @Test
    public void constructorFailsFastWhenOAuthConfigurationIsMissing() throws MalformedURLException {
        var startupProperties = mock(ApplicationProperties.class);
        when(startupProperties.getTokenUrl()).thenReturn(null);
        when(startupProperties.getClientId()).thenReturn("local-connector-example");
        when(startupProperties.getClientSecret()).thenReturn("secret");

        assertThrows(IllegalStateException.class, () ->
                new HttpPegelHubCommunicator(httpClient, baseUrl(), startupProperties));
    }

    @Nested
    @DisplayName("Connector API Tests")
    class ConnectorAPITest {
        @Test
        public void getConnectors_NotEmptyCollectionWhenData() throws IOException {
            mockSuccessfulResponse(getResource("ConnectorsFilledResponse.json"));

            Collection<Connector> connectors = phc.getConnectors();

            assertFalse(connectors.isEmpty());
        }

        @Test
        public void getConnectors_EmptyCollectionWhenNoData() throws IOException {
            mockSuccessfulResponse(getResource("EmptyArray.json"));

            Collection<Connector> connectors = phc.getConnectors();

            assertTrue(connectors.isEmpty());
        }

        @Test
        public void getConnectorByUUID_ValueWhenData() throws IOException {
            mockSuccessfulResponse(getResource("Connector.json"));

            Optional<Connector> response = phc.getConnectorByUUID(uuid);

            assertTrue(response.isPresent());
        }

        @Test
        public void getConnectorByUUID_EmptyWhenNoData() throws IOException {
            mockSuccessfulResponse(getResource("EmptyResponse.json"));

            Optional<Connector> response = phc.getConnectorByUUID(UUID.randomUUID());

            assertTrue(response.isEmpty());
        }

        @Test
        public void getConnectorByUUID_ThrowsWhenConnectionError() {
            assertThrows(Exception.class, () -> {
                mockFailedResponse(HttpStatus.SC_REQUEST_TIMEOUT);
                phc.getConnectorByUUID(uuid);
            });
        }

        @Test
        public void sendConnector_DoesNotThrowWhenHandlingOKResponse() throws IOException {
            mockSuccessfulResponse(getResource("EmptyResponse.json"));

            assertDoesNotThrow(() -> {
                var con = new Connector();
                phc.sendConnector(con);
            });
        }

        @Test
        public void sendConnector_ThrowsWhenHandlingBadResponse() throws IOException {
            mockFailedResponse(400);

            assertThrows(Exception.class, () -> {
                var con = new Connector();
                phc.sendConnector(con);
            });
        }
    }

    @Nested
    @DisplayName("Contact API Tests")
    class ContactAPITest {
        @Test
        public void getContacts_FilledCollectionWhenData() throws IOException {
            mockSuccessfulResponse(getResource("ContactsFilledResponse.json"));

            Collection<Contact> contacts = phc.getContacts();

            assertFalse(contacts.isEmpty());
        }

        @Test
        public void getContacts_EmptyCollectionWhenNoData() throws IOException {
            mockSuccessfulResponse(getResource("EmptyArray.json"));

            Collection<Contact> contacts = phc.getContacts();

            assertTrue(contacts.isEmpty());
        }

        @Test
        public void getContactByUUID_ValueWhenData() throws IOException {
            mockSuccessfulResponse(getResource("Contact.json"));

            Optional<Contact> response = phc.getContactByUUID(uuid);

            assertTrue(response.isPresent());
        }

        @Test
        public void getContactByUUID_EmptyWhenNoData() throws IOException {
            mockSuccessfulResponse(getResource("EmptyResponse.json"));

            Optional<Contact> response = phc.getContactByUUID(UUID.randomUUID());

            assertTrue(response.isEmpty());
        }

        @Test
        public void getContactByUUID_ThrowsWhenConnectionError() {
            assertThrows(Exception.class, () -> {
                mockFailedResponse(HttpStatus.SC_REQUEST_TIMEOUT);
                phc.getContactByUUID(uuid);
            });
        }

        @Test
        public void sendContact_DoesNotThrowWhenHandlingOKResponse() throws IOException {
            mockSuccessfulResponse(getResource("EmptyResponse.json"));

            assertDoesNotThrow(() -> {
                var con = new Contact();
                phc.sendContact(con);
            });
        }

        @Test
        public void sendContact_ThrowsWhenHandlingBadResponse() throws IOException {
            mockFailedResponse(400);

            assertThrows(Exception.class, () -> {
                var con = new Contact();
                phc.sendContact(con);
            });
        }
    }

    @Nested
    @DisplayName("Measurement API Tests")
    class MeasurementAPITest {
        @Test
        public void getMeasurementsOfTimeSeries_UsesTimeSeriesRoute() throws IOException {
            when(properties.isSupplier()).thenReturn(false);
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
                    "http://localhost:1111/api/v1/time-series/395c0232-d110-40fd-bd7f-2bb4a0f2009d/measurements/72h",
                    requestUris.get(1));
        }

        @Test
        public void getLatestMeasurementOfTimeSeries_UsesTimeSeriesRoute() throws IOException {
            when(properties.isSupplier()).thenReturn(false);
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
                    "http://localhost:1111/api/v1/time-series/395c0232-d110-40fd-bd7f-2bb4a0f2009d/measurements/latest",
                    requestUris.get(1));
        }

        @Test
        public void sendMeasurements_DoesNotThrowWhenHandlingOKResponse() throws IOException {
            mockSuccessfulResponse(getResource("EmptyResponse.json"));
            when(properties.isSupplier()).thenReturn(true);

            assertDoesNotThrow(() -> {
                var meas = measurement();
                phc.sendMeasurements(List.of(meas));
            });
        }

        @Test
        public void sendMeasurements_SerializesCleanMeasurementAsUtcJson() throws IOException {
            when(properties.isSupplier()).thenReturn(true);
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
            when(properties.isSupplier()).thenReturn(true);

            assertThrows(Exception.class, () -> {
                var meas = measurement();
                phc.sendMeasurements(List.of(meas));
            });
        }

        @Test
        public void sendMeasurements_ThrowsWhenMeasurementIsMissingTimeSeriesId() {
            when(properties.isSupplier()).thenReturn(true);

            assertThrows(RuntimeException.class, () -> phc.sendMeasurements(List.of(new Measurement())));
        }

        private Measurement measurement() {
            return new Measurement(
                    UUID.fromString("395c0232-d110-40fd-bd7f-2bb4a0f2009d"),
                    Instant.parse("2026-04-25T10:15:30Z"),
                    1.0);
        }
    }

    private void mockSuccessfulResponse(String response) throws IOException {
        when(httpClient.execute(any(), any(HttpClientResponseHandler.class))).thenAnswer(a -> {
            var request = (org.apache.hc.client5.http.classic.methods.HttpUriRequestBase) a.getRawArguments()[0];
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
