package at.pegelhub.shared.web;

import at.pegelhub.connector.api.HttpAdminConnectorController;
import at.pegelhub.connector.api.HttpConnectorController;
import at.pegelhub.connector.application.ConnectorService;
import at.pegelhub.contact.api.HttpContactController;
import at.pegelhub.contact.application.ContactService;
import at.pegelhub.measurement.api.MeasurementController;
import at.pegelhub.measurement.api.read.MeasurementReadQueryResolver;
import at.pegelhub.measurement.application.MeasurementBucketList;
import at.pegelhub.measurement.application.MeasurementBucketResolutionPolicy;
import at.pegelhub.measurement.application.MeasurementList;
import at.pegelhub.measurement.application.MeasurementService;
import at.pegelhub.measurement.domain.MeasurementBucket;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static at.pegelhub.testsupport.ExampleData.CONTACT;
import static at.pegelhub.testsupport.ExampleData.CONNECTOR;
import static at.pegelhub.testsupport.ExampleData.ID;
import static at.pegelhub.testsupport.ExampleData.MEASUREMENT;
import static at.pegelhub.testsupport.ExampleData.MEASUREMENT_PAGE_ROWS;
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
        MeasurementController.class,
        HttpTelemetryController.class,
        HttpAdminConnectorController.class,
        HttpConnectorController.class,
        HttpContactController.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import({MeasurementReadQueryResolver.class, MeasurementBucketResolutionPolicy.class})
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

    @MockitoBean
    private Clock clock;

    @BeforeEach
    void prepare() {
        when(clock.instant()).thenReturn(Instant.parse("2026-06-17T13:00:00Z"));
        when(measurementService.listMeasurements(any())).thenAnswer(invocation ->
                new MeasurementList(invocation.getArgument(0), false, null, MEASUREMENT_PAGE_ROWS));
        when(measurementService.listMeasurementBuckets(any())).thenAnswer(invocation ->
                new MeasurementBucketList(invocation.getArgument(0), Collections.singletonList(new MeasurementBucket(
                        MEASUREMENT.timeSeriesId(),
                        Instant.parse("2026-06-17T12:00:00Z"),
                        Instant.parse("2026-06-17T12:05:00Z"),
                        1.0,
                        1))));
        when(measurementService.getSystemTime()).thenReturn(Instant.parse("2026-01-02T03:04:05Z"));

        when(telemetryService.saveTelemetry(any())).thenReturn(TELEMETRY);
        when(telemetryService.getByRange(anyString())).thenReturn(TELEMETRIES);
        when(telemetryService.getLastData(any())).thenReturn(TELEMETRY);

        when(connectorService.create(any())).thenReturn(CONNECTOR);
        when(connectorService.get(any())).thenReturn(CONNECTOR);
        when(connectorService.list()).thenReturn(List.of(CONNECTOR));

        when(contactService.createContact(any())).thenReturn(CONTACT);
        when(contactService.getContactById(any())).thenReturn(CONTACT);
        when(contactService.getAllContacts()).thenReturn(List.of(CONTACT));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("migratedEndpoints")
    void migratedEndpointsReachControllerWithoutApiKey(EndpointCase endpointCase) throws Exception {
        mockMvc.perform(endpointCase.request().get())
                .andExpect(status().is2xxSuccessful());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("apiControllers")
    void apiControllersDoNotDeclareApiKeyRequestParams(Class<?> controllerType) {
        assertNoApiKeyRequestParams(controllerType);
    }

    private static void assertNoApiKeyRequestParams(Class<?> controllerType) {
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
        for (Class<?> apiContract : controllerType.getInterfaces()) {
            assertNoApiKeyRequestParams(apiContract);
        }
    }

    private static Stream<EndpointCase> migratedEndpoints() {
        return Stream.of(
                new EndpointCase("measurement POST", () -> post("/api/v1/measurements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(measurementsJson())),
                new EndpointCase("measurement GET raw time series window", () -> get("/api/v1/time-series/{timeSeriesId}/measurements", TIME_SERIES_ID.value())
                        .param("last", "72h")),
                new EndpointCase("measurement GET bucketed time series window", () -> get("/api/v1/time-series/{timeSeriesId}/measurements/buckets", TIME_SERIES_ID.value())
                        .param("last", "72h")
                        .param("bucket", "5m")),
                new EndpointCase("measurement system time", () -> get("/api/v1/measurements/system-time")),
                new EndpointCase("telemetry POST", () -> post("/api/v1/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(telemetryJson())),
                new EndpointCase("telemetry GET range", () -> get("/api/v1/telemetry/{range}", "72h")),
                new EndpointCase("telemetry GET last", () -> get("/api/v1/telemetry/last/{uuid}", ID))
        );
    }

    private static Stream<Class<?>> apiControllers() {
        return Stream.of(
                MeasurementController.class,
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
