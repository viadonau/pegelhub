package at.pegelhub.shared.web;

import at.pegelhub.connector.api.HttpAdminConnectorController;
import at.pegelhub.connector.api.HttpConnectorController;
import at.pegelhub.connector.application.ConnectorService;
import at.pegelhub.contact.api.HttpContactController;
import at.pegelhub.contact.application.ContactService;
import at.pegelhub.measurement.api.HttpMeasurementController;
import at.pegelhub.measurement.application.MeasurementService;
import at.pegelhub.supplier.api.HttpStationManufacturerController;
import at.pegelhub.supplier.api.HttpSupplierController;
import at.pegelhub.supplier.application.StationManufacturerService;
import at.pegelhub.supplier.application.SupplierService;
import at.pegelhub.taker.api.HttpTakerController;
import at.pegelhub.taker.api.HttpTakerServiceManufacturerController;
import at.pegelhub.taker.application.TakerService;
import at.pegelhub.taker.application.TakerServiceManufacturerService;
import at.pegelhub.telemetry.api.HttpTelemetryController;
import at.pegelhub.telemetry.application.TelemetryService;
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
import java.util.function.Supplier;
import java.util.stream.Stream;

import static at.pegelhub.testsupport.ExampleData.CONTACT;
import static at.pegelhub.testsupport.ExampleData.CONNECTOR;
import static at.pegelhub.testsupport.ExampleData.ID;
import static at.pegelhub.testsupport.ExampleData.MEASUREMENT;
import static at.pegelhub.testsupport.ExampleData.MEASUREMENTS;
import static at.pegelhub.testsupport.ExampleData.STATION_MANUFACTURER;
import static at.pegelhub.testsupport.ExampleData.SUPPLIER;
import static at.pegelhub.testsupport.ExampleData.TAKER;
import static at.pegelhub.testsupport.ExampleData.TAKER_SERVICE_MANUFACTURER;
import static at.pegelhub.testsupport.ExampleData.TELEMETRIES;
import static at.pegelhub.testsupport.ExampleData.TELEMETRY;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
        HttpMeasurementController.class,
        HttpTelemetryController.class,
        HttpSupplierController.class,
        HttpTakerController.class,
        HttpAdminConnectorController.class,
        HttpConnectorController.class,
        HttpContactController.class,
        HttpStationManufacturerController.class,
        HttpTakerServiceManufacturerController.class
})
@AutoConfigureMockMvc(addFilters = false)
class AuthPolicyMatrixWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MeasurementService measurementService;

    @MockitoBean
    private TelemetryService telemetryService;

    @MockitoBean
    private SupplierService supplierService;

    @MockitoBean
    private TakerService takerService;

    @MockitoBean
    private ConnectorService connectorService;

    @MockitoBean
    private ContactService contactService;

    @MockitoBean
    private StationManufacturerService stationManufacturerService;

    @MockitoBean
    private TakerServiceManufacturerService takerServiceManufacturerService;

    @BeforeEach
    void prepare() {
        when(measurementService.getByRange(anyString())).thenReturn(MEASUREMENTS);
        when(measurementService.getBySupplierAndRange(anyString(), anyString())).thenReturn(MEASUREMENTS);
        when(measurementService.getLatestBySupplier(anyString())).thenReturn(MEASUREMENT);
        when(measurementService.getAverageBySupplierAndRange(anyString(), anyString())).thenReturn(MEASUREMENT);
        when(measurementService.getLastData(any())).thenReturn(MEASUREMENT);
        when(measurementService.getSystemTime()).thenReturn(Instant.parse("2026-01-02T03:04:05Z"));

        when(telemetryService.saveTelemetry(any())).thenReturn(TELEMETRY);
        when(telemetryService.getByRange(anyString())).thenReturn(TELEMETRIES);
        when(telemetryService.getLastData(any())).thenReturn(TELEMETRY);

        when(supplierService.saveSupplier(any())).thenReturn(SUPPLIER);
        when(supplierService.updateSupplier(any())).thenReturn(SUPPLIER);
        when(supplierService.getSupplierById(any())).thenReturn(SUPPLIER);
        when(supplierService.getAllSuppliers()).thenReturn(List.of(SUPPLIER));
        when(supplierService.getConnectorID(any())).thenReturn(CONNECTOR.getId());

        when(takerService.saveTaker(any())).thenReturn(TAKER);
        when(takerService.getTakerById(any())).thenReturn(TAKER);
        when(takerService.getAllTakers()).thenReturn(List.of(TAKER));

        when(connectorService.createConnector(any())).thenReturn(CONNECTOR);
        when(connectorService.getConnectorById(any())).thenReturn(CONNECTOR);
        when(connectorService.getAllConnectors()).thenReturn(List.of(CONNECTOR));

        when(contactService.createContact(any())).thenReturn(CONTACT);
        when(contactService.getContactById(any())).thenReturn(CONTACT);
        when(contactService.getAllContacts()).thenReturn(List.of(CONTACT));

        when(stationManufacturerService.createStationManufacturer(any())).thenReturn(STATION_MANUFACTURER);
        when(stationManufacturerService.getStationManufacturerById(any())).thenReturn(STATION_MANUFACTURER);
        when(stationManufacturerService.getAllStationManufacturers()).thenReturn(List.of(STATION_MANUFACTURER));

        when(takerServiceManufacturerService.createTakerServiceManufacturer(any())).thenReturn(TAKER_SERVICE_MANUFACTURER);
        when(takerServiceManufacturerService.getTakerServiceManufacturerById(any())).thenReturn(TAKER_SERVICE_MANUFACTURER);
        when(takerServiceManufacturerService.getAllTakerServiceManufacturers()).thenReturn(List.of(TAKER_SERVICE_MANUFACTURER));
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
                new EndpointCase("measurement GET range", () -> get("/api/v1/measurement/{range}", "72h")),
                new EndpointCase("measurement GET supplier range", () -> get("/api/v1/measurement/supplier/{range}", "72h")
                        .param("stationNumber", "stationNR")),
                new EndpointCase("measurement GET supplier latest", () -> get("/api/v1/measurement/supplier/latest")
                        .param("stationNumber", "stationNR")),
                new EndpointCase("measurement GET supplier average", () -> get("/api/v1/measurement/supplier/average/{range}", "72h")
                        .param("stationNumber", "stationNR")),
                new EndpointCase("measurement GET last", () -> get("/api/v1/measurement/last/{uuid}", ID)),
                new EndpointCase("measurement system time", () -> get("/api/v1/measurement/systemTime")),
                new EndpointCase("telemetry POST", () -> post("/api/v1/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(telemetryJson())),
                new EndpointCase("telemetry GET range", () -> get("/api/v1/telemetry/{range}", "72h")),
                new EndpointCase("telemetry GET last", () -> get("/api/v1/telemetry/last/{uuid}", ID)),
                new EndpointCase("supplier POST", () -> post("/api/v1/supplier")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(supplierJson())),
                new EndpointCase("supplier PUT", () -> put("/api/v1/supplier")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(supplierJson())),
                new EndpointCase("supplier GET one", () -> get("/api/v1/supplier/{uuid}", ID)),
                new EndpointCase("supplier GET all", () -> get("/api/v1/supplier")),
                new EndpointCase("supplier DELETE", () -> delete("/api/v1/supplier/{uuid}", ID)),
                new EndpointCase("supplier connector id", () -> get("/api/v1/supplier/connectorID/{uuid}", ID)),
                new EndpointCase("taker POST", () -> post("/api/v1/taker")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(takerJson())),
                new EndpointCase("taker GET one", () -> get("/api/v1/taker/{uuid}", ID)),
                new EndpointCase("taker GET all", () -> get("/api/v1/taker")),
                new EndpointCase("taker DELETE", () -> delete("/api/v1/taker/{uuid}", ID))
        );
    }

    private static Stream<Class<?>> apiControllers() {
        return Stream.of(
                HttpMeasurementController.class,
                HttpTelemetryController.class,
                HttpSupplierController.class,
                HttpTakerController.class,
                HttpAdminConnectorController.class,
                HttpConnectorController.class,
                HttpContactController.class,
                HttpStationManufacturerController.class,
                HttpTakerServiceManufacturerController.class
        );
    }

    private static String measurementsJson() {
        return """
                {
                  "measurements": [
                    {
                      "timestamp": "2026-04-25T10:15:30Z",
                      "fields": {
                        "waterLevel": 10.5,
                        "flow": 20.5
                      },
                      "infos": {
                        "quality": "ok"
                      }
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

    private static String supplierJson() {
        return """
                {
                  "stationNumber": "stationNR",
                  "stationId": 4143365,
                  "stationName": "stationName",
                  "stationWater": "stationWater",
                  "stationWaterType": "t",
                  "stationManufacturer": {
                    "stationManufacturerName": "name",
                    "stationManufacturerType": "type",
                    "stationManufacturerFirmwareVersion": "1.0.0",
                    "stationRemark": "remarks"
                  },
                  "connector": {
                    "connectorNumber": "connectorNR",
                    "manufacturer": {
                      "organization": "org1"
                    },
                    "typeDescription": "description",
                    "softwareVersion": "1.0.0",
                    "worksFromDataVersion": "1.0.0",
                    "dataDefinition": "definition",
                    "softwareManufacturer": {
                      "organization": "org1"
                    },
                    "technicallyResponsible": {
                      "organization": "org1"
                    },
                    "operationCompany": {
                      "organization": "org1"
                    },
                    "notes": "notes"
                  },
                  "refreshRate": 100,
                  "accuracy": 1.0,
                  "mainUsage": "do smth",
                  "dataCritically": "true",
                  "stationBaseReferenceLevel": 0.5,
                  "stationReferencePlace": "place",
                  "stationWaterKilometer": 50.0,
                  "stationWaterSide": "side",
                  "stationWaterLatitude": 45.5,
                  "stationWaterLongitude": 45.5,
                  "stationWaterLatitudem": 45.5,
                  "stationWaterLongitudem": 45.5,
                  "hsw100": 45.5,
                  "hsw": 45.5,
                  "hswReference": 45,
                  "mw": 45.5,
                  "mwReference": 45,
                  "rnw": 45.5,
                  "rnwReference": 45,
                  "hsq100": 45.5,
                  "hsq": 45.5,
                  "mq": 45.5,
                  "rnq": 45.5,
                  "channelUse": "TestUse",
                  "utcIsUsed": false,
                  "isSummertime": false
                }
                """;
    }

    private static String takerJson() {
        return """
                {
                  "stationNumber": "stationNR",
                  "stationId": 4143365,
                  "takerServiceManufacturer": {
                    "takerManufacturerName": "name",
                    "takerSystemName": "name",
                    "stationManufacturerFirmwareVersion": "1.0.0",
                    "requestRemark": "remarks"
                  },
                  "connector": {
                    "connectorNumber": "connectorNR",
                    "manufacturer": {
                      "organization": "org1"
                    },
                    "typeDescription": "description",
                    "softwareVersion": "1.0.0",
                    "worksFromDataVersion": "1.0.0",
                    "dataDefinition": "definition",
                    "softwareManufacturer": {
                      "organization": "org1"
                    },
                    "technicallyResponsible": {
                      "organization": "org1"
                    },
                    "operationCompany": {
                      "organization": "org1"
                    },
                    "notes": "notes"
                  },
                  "refreshRate": 100
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
