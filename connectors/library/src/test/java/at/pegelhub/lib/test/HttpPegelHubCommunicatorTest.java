package at.pegelhub.lib.test;

import at.pegelhub.lib.internal.ApplicationProperties;
import at.pegelhub.lib.internal.HttpPegelHubCommunicator;
import at.pegelhub.lib.model.Connector;
import at.pegelhub.lib.model.Contact;
import at.pegelhub.lib.model.Measurement;
import at.pegelhub.lib.model.Telemetry;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.junit.jupiter.api.*;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
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
        phc = new HttpPegelHubCommunicator(httpClient, new URL("http://localhost:1111/"), properties);
        uuid = UUID.fromString("74bcffac-8fa6-41ac-aa9d-53d082447226");
    }

    @Test
    public void httpClientIsClosedOnCloseAndDoesNotThrow() {
        assertDoesNotThrow(() -> {
            phc.close();
            verify(httpClient, times(1)).close();
        });
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
        public void getMeasurements_FilledCollectionWhenData() throws IOException {
            mockSuccessfulResponse(getResource("MeasurementsFilledResponse.json"));
            when(properties.isSupplier()).thenReturn(false);

            Collection<Measurement> measurements = phc.getMeasurements("");

            assertFalse(measurements.isEmpty());
        }

        @Test
        public void getMeasurements_EmptyCollectionWhenNoData() throws IOException {
            mockSuccessfulResponse(getResource("EmptyArray.json"));
            when(properties.isSupplier()).thenReturn(false);

            Collection<Measurement> measurements = phc.getMeasurements("");

            assertTrue(measurements.isEmpty());
        }

        @Test
        public void getMeasurements_ThrowsWhenNotTaker() throws IOException {
            mockSuccessfulResponse(getResource("EmptyObject.json"));
            when(properties.isSupplier()).thenReturn(true);

            assertThrows(Exception.class, () -> phc.getMeasurements(""));
        }

        @Test
        public void getMeasurementByUUID_ValueWhenData() throws IOException {
            mockSuccessfulResponse(getResource("Measurement.json"));
            when(properties.isSupplier()).thenReturn(false);

            Optional<Measurement> measurement = phc.getMeasurementByUUID(uuid);

            assertTrue(measurement.isPresent());
        }

        @Test
        public void getMeasurementByUUID_EmptyWhenNoData() throws IOException {
            when(properties.isSupplier()).thenReturn(false);

            Optional<Measurement> measurement = phc.getMeasurementByUUID(uuid);

            assertTrue(measurement.isEmpty());
        }

        @Test
        public void getMeasurementByUUID_ThrowsWhenConnectionError() {
            when(properties.isSupplier()).thenReturn(false);
            assertThrows(Exception.class, () -> {
                mockFailedResponse(HttpStatus.SC_REQUEST_TIMEOUT);
                phc.getMeasurementByUUID(uuid);
            });
        }

        @Test
        public void getMeasurementByUUID_ThrowsWhenNotTaker() {
            when(properties.isSupplier()).thenReturn(true);
            assertThrows(RuntimeException.class, () -> phc.getMeasurementByUUID(uuid));
        }

        @Test
        public void sendMeasurements_DoesNotThrowWhenHandlingOKResponse() throws IOException {
            mockSuccessfulResponse(getResource("EmptyResponse.json"));
            when(properties.isSupplier()).thenReturn(true);

            assertDoesNotThrow(() -> {
                var meas = new Measurement();
                phc.sendMeasurements(List.of(meas));
            });
        }

        @Test
        public void sendMeasurements_ThrowsWhenHandlingBadResponse() throws IOException {
            mockFailedResponse(400);
            when(properties.isSupplier()).thenReturn(true);

            assertThrows(Exception.class, () -> {
                var meas = new Measurement();
                phc.sendMeasurements(List.of(meas));
            });
        }

        @Test
        public void sendMeasurements_ThrowsWhenNotSupplier() throws IOException {
            mockFailedResponse(400);
            when(properties.isSupplier()).thenReturn(false);

            assertThrows(RuntimeException.class, () -> {
                var meas = new Measurement();
                phc.sendMeasurements(List.of(meas));
            });
        }
    }

    @Nested
    @DisplayName("Telemetry API Tests")
    class TelemetryAPITest {
        @Test
        public void getTelemetry_FilledCollectionWhenData() throws IOException {
            mockSuccessfulResponse(getResource("TelemetryFilledResponse.json"));
            when(properties.isSupplier()).thenReturn(false);

            Collection<Telemetry> telemetries = phc.getTelemetry("");

            assertFalse(telemetries.isEmpty());
        }

        @Test
        public void getTelemetry_EmptyCollectionWhenNoData() throws IOException {
            mockSuccessfulResponse(getResource("EmptyObject.json"));
            when(properties.isSupplier()).thenReturn(false);

            Collection<Telemetry> telemetries = phc.getTelemetry("7d");

            assertTrue(telemetries.isEmpty());
        }

        @Test
        public void getTelemetry_ThrowsWhenNotTaker() throws IOException {
            mockSuccessfulResponse(getResource("EmptyObject.json"));
            when(properties.isSupplier()).thenReturn(true);

            assertThrows(Exception.class, () -> {
                mockFailedResponse(HttpStatus.SC_REQUEST_TIMEOUT);
                phc.getTelemetry("7d");
            });
        }

        @Test
        public void getTelemetryByUUID_ValueWhenData() throws IOException {
            mockSuccessfulResponse(getResource("Telemetry.json"));
            when(properties.isSupplier()).thenReturn(false);

            Optional<Telemetry> telemetry = phc.getTelemetryByUUID(uuid);

            assertTrue(telemetry.isPresent());
        }

        @Test
        public void getTelemetryByUUID_EmptyWhenNoData() throws IOException {
            mockSuccessfulResponse(getResource("EmptyObject.json"));
            when(properties.isSupplier()).thenReturn(false);

            Optional<Telemetry> telemetry = phc.getTelemetryByUUID(uuid);

            assertTrue(telemetry.isEmpty());
        }

        @Test
        public void getTelemetryByUUID_ThrowsWhenConnectionError() {
            when(properties.isSupplier()).thenReturn(false);
            assertThrows(Exception.class, () -> {
                mockFailedResponse(HttpStatus.SC_REQUEST_TIMEOUT);
                phc.getTelemetryByUUID(uuid);
            });
        }

        @Test
        public void getTelemetryByUUID_ThrowsWhenNotTaker() {
            when(properties.isSupplier()).thenReturn(true);
            assertThrows(RuntimeException.class, () -> phc.getTelemetryByUUID(uuid));
        }

        @Test
        public void sendTelemetry_DoesNotThrowWhenHandlingOKResponse() throws IOException {
            mockSuccessfulResponse(getResource("EmptyResponse.json"));
            when(properties.isSupplier()).thenReturn(true);

            assertDoesNotThrow(() -> {
                var tel = new Telemetry();
                phc.sendTelemetry(tel);
            });
        }

        @Test
        public void sendTelemetry_ThrowsWhenHandlingBadResponse() throws IOException {
            mockFailedResponse(400);
            when(properties.isSupplier()).thenReturn(true);

            assertThrows(Exception.class, () -> {
                var tel = new Telemetry();
                phc.sendTelemetry(tel);
            });
        }

        @Test
        public void sendTelemetry_ThrowsWhenNotSupplier() {
            when(properties.isSupplier()).thenReturn(false);

            assertThrows(RuntimeException.class, () -> {
                var tel = new Telemetry();
                phc.sendTelemetry(tel);
            });
        }
    }

    private void mockSuccessfulResponse(String response) throws IOException {
        when(httpClient.execute(any(), any(HttpClientResponseHandler.class))).thenAnswer(a -> {
            var responseCallback = (HttpClientResponseHandler<?>) a.getRawArguments()[1];
            ClassicHttpResponse httpResp = mock(ClassicHttpResponse.class);
            HttpEntity entity = mock(HttpEntity.class);
            when(entity.getContent()).thenReturn(new ByteArrayInputStream(response.getBytes()));
            when(httpResp.getEntity()).thenReturn(entity);
            when(httpResp.getCode()).thenReturn(HttpStatus.SC_OK);
            return responseCallback.handleResponse(httpResp);
        });
    }

    private void mockFailedResponse(int code) throws IOException {
        when(httpClient.execute(any(), any(HttpClientResponseHandler.class))).thenAnswer(a -> {
            var responseCallback = (HttpClientResponseHandler<?>) a.getRawArguments()[1];
            ClassicHttpResponse httpResp = mock(ClassicHttpResponse.class);
            when(httpResp.getCode()).thenReturn(code);
            return responseCallback.handleResponse(httpResp);
        });
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
