package at.pegelhub.shared.web;

import at.pegelhub.connector.api.HttpAdminConnectorController;
import at.pegelhub.connector.api.HttpConnectorController;
import at.pegelhub.connector.application.ConnectorService;
import at.pegelhub.contact.api.HttpContactController;
import at.pegelhub.contact.application.ContactService;
import at.pegelhub.measurement.api.HttpMeasurementController;
import at.pegelhub.measurement.application.MeasurementService;
import at.pegelhub.telemetry.api.HttpTelemetryController;
import at.pegelhub.telemetry.application.TelemetryService;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static at.pegelhub.testsupport.ExampleData.CONTACT;
import static at.pegelhub.testsupport.ExampleData.CONNECTOR;
import static at.pegelhub.testsupport.ExampleData.ID;
import static at.pegelhub.testsupport.ExampleData.MEASUREMENT;
import static at.pegelhub.testsupport.ExampleData.MEASUREMENTS;
import static at.pegelhub.testsupport.ExampleData.TELEMETRIES;
import static at.pegelhub.testsupport.ExampleData.TELEMETRY;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
        HttpMeasurementController.class,
        HttpTelemetryController.class,
        HttpAdminConnectorController.class,
        HttpConnectorController.class,
        HttpContactController.class
})
@AutoConfigureMockMvc(addFilters = false)
class AuthPolicyMatrixWebMvcTest {

    private static final TimeSeriesId TIME_SERIES_ID = new TimeSeriesId(UUID.fromString("8ce8c5b6-f093-4d46-b770-7239cdfa3d76"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MeasurementService measurementService;

    @MockitoBean
    private TelemetryService telemetryService;

    @MockitoBean
    private ConnectorService connectorService;

    @MockitoBean
    private ContactService contactService;

    @BeforeEach
    void prepare() {
        when(measurementService.getByTimeSeriesAndRange(any(), anyString())).thenReturn(MEASUREMENTS);
        when(measurementService.getLatestByTimeSeries(any())).thenReturn(MEASUREMENT);
        when(measurementService.getAverageByTimeSeriesAndRange(any(), anyString())).thenReturn(MEASUREMENT);
        when(measurementService.getSystemTime()).thenReturn(Instant.parse("2026-01-02T03:04:05Z"));

        when(telemetryService.saveTelemetry(any())).thenReturn(TELEMETRY);
        when(telemetryService.getByRange(anyString())).thenReturn(TELEMETRIES);
        when(telemetryService.getLastData(any())).thenReturn(TELEMETRY);

        when(connectorService.createConnector(any())).thenReturn(CONNECTOR);
        when(connectorService.getConnectorById(any())).thenReturn(CONNECTOR);
        when(connectorService.getAllConnectors()).thenReturn(List.of(CONNECTOR));

        when(contactService.createContact(any())).thenReturn(CONTACT);
        when(contactService.getContactById(any())).thenReturn(CONTACT);
        when(contactService.getAllContacts()).thenReturn(List.of(CONTACT));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("migratedEndpoints")
    void migratedEndpointsReachControllerWithoutApiKey(EndpointCase endpointCase) throws Exception {
        mockMvc.perform(endpointCase.request().get())
                .andExpect(status().isOk());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("apiControllers")
    void apiControllersDoNotDeclareApiKeyRequestParams(Class<?> controllerType) {
        for (Method method : controllerType.getDeclaredMethods()) {
            for (Parameter parameter : method.getParameters()) {
                RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
                if (requestParam != null) {
                    assertNotEquals("apiKey", requestParam.name(), method.toGenericString());
                    assertNotEquals("apiKey", requestParam.value(), method.toGenericString());
                    assertNotEquals("apiKey", parameter.getName(), method.toGenericString());
                }
            }
        }
    }

    private static Stream<EndpointCase> migratedEndpoints() {
        return Stream.of(
                new EndpointCase("measurement POST", () -> post("/api/v1/measurement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(measurementsJson())),
                new EndpointCase("measurement GET time series range", () -> get("/api/v1/measurement/time-series/{timeSeriesId}/{range}", TIME_SERIES_ID.value(), "72h")),
                new EndpointCase("measurement GET time series latest", () -> get("/api/v1/measurement/time-series/{timeSeriesId}/latest", TIME_SERIES_ID.value())),
                new EndpointCase("measurement GET time series average", () -> get("/api/v1/measurement/time-series/{timeSeriesId}/average/{range}", TIME_SERIES_ID.value(), "72h")),
                new EndpointCase("measurement system time", () -> get("/api/v1/measurement/systemTime")),
                new EndpointCase("telemetry POST", () -> post("/api/v1/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(telemetryJson())),
                new EndpointCase("telemetry GET range", () -> get("/api/v1/telemetry/{range}", "72h")),
                new EndpointCase("telemetry GET last", () -> get("/api/v1/telemetry/last/{uuid}", ID))
        );
    }

    private static Stream<Class<?>> apiControllers() {
        return Stream.of(
                HttpMeasurementController.class,
                HttpTelemetryController.class,
                HttpAdminConnectorController.class,
                HttpConnectorController.class,
                HttpContactController.class
        );
    }

    private static String measurementsJson() {
        return """
                {
                  "measurements": [
                    {
                      "timeSeriesId": "8ce8c5b6-f093-4d46-b770-7239cdfa3d76",
                      "observedAt": "2026-04-25T10:15:30Z",
                      "value": 10.5
                    }
                  ]
                }
                """;
    }

    private static String telemetryJson() {
        return """
                {
                  "measurement": "a93fdc3d-b71f-44ce-a826-fe1dc1f1f357",
                  "stationIPAddressIntern": "172.0.0.0",
                  "stationIPAddressExtern": "172.0.0.0",
                  "timestamp": "2010-10-12T08:50:00Z",
                  "cycleTime": 1,
                  "temperatureWater": -2.0,
                  "temperatureAir": -2.0,
                  "performanceVoltageBattery": 2.0,
                  "performanceVoltageSupply": 2.0,
                  "performanceElectricityBattery": 2.0,
                  "performanceElectricitySupply": 2.0,
                  "fieldStrengthTransmission": 2.0
                }
                """;
    }

    private record EndpointCase(String name, Supplier<MockHttpServletRequestBuilder> request) {
        @Override
        public @NonNull String toString() {
            return name;
        }
    }
}
