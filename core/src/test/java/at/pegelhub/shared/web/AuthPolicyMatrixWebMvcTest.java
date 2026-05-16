package at.pegelhub.shared.web;

import at.pegelhub.auth.api.HttpApiTokenController;
import at.pegelhub.auth.application.ApiTokenService;
import at.pegelhub.auth.application.AuthorizationService;
import at.pegelhub.connector.api.HttpConnectorController;
import at.pegelhub.connector.application.ConnectorService;
import at.pegelhub.contact.api.HttpContactController;
import at.pegelhub.contact.application.ContactService;
import at.pegelhub.measurement.api.HttpMeasurementController;
import at.pegelhub.measurement.application.MeasurementService;
import at.pegelhub.shared.error.UnauthorizedException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static at.pegelhub.testsupport.ExampleData.CONTACT;
import static at.pegelhub.testsupport.ExampleData.CONNECTOR;
import static at.pegelhub.testsupport.ExampleData.ID;
import static at.pegelhub.testsupport.ExampleData.STATION_MANUFACTURER;
import static at.pegelhub.testsupport.ExampleData.SUPPLIER;
import static at.pegelhub.testsupport.ExampleData.TAKER;
import static at.pegelhub.testsupport.ExampleData.TAKER_SERVICE_MANUFACTURER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
        HttpMeasurementController.class,
        HttpTelemetryController.class,
        HttpSupplierController.class,
        HttpTakerController.class,
        HttpApiTokenController.class,
        HttpConnectorController.class,
        HttpContactController.class,
        HttpStationManufacturerController.class,
        HttpTakerServiceManufacturerController.class
})
class AuthPolicyMatrixWebMvcTest {

    private static final String TOKEN = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthorizationService authorizationService;

    @MockitoBean
    private MeasurementService measurementService;

    @MockitoBean
    private TelemetryService telemetryService;

    @MockitoBean
    private SupplierService supplierService;

    @MockitoBean
    private TakerService takerService;

    @MockitoBean
    private ApiTokenService apiTokenService;

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
        doThrow(new UnauthorizedException("unauthorized")).when(authorizationService).authorize(anyString());

        when(apiTokenService.createToken()).thenReturn(TOKEN);
        when(apiTokenService.refreshToken(anyString(), any())).thenReturn(TOKEN);
        when(apiTokenService.getTokens()).thenReturn(List.of(ID));

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

        when(supplierService.getSupplierById(any())).thenReturn(SUPPLIER);
        when(supplierService.getAllSuppliers()).thenReturn(List.of(SUPPLIER));
        when(supplierService.getConnectorID(any())).thenReturn(CONNECTOR.getId());

        when(takerService.getTakerById(any())).thenReturn(TAKER);
        when(takerService.getAllTakers()).thenReturn(List.of(TAKER));

        when(measurementService.getSystemTime()).thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 1, 2, 3, 4, 5)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("protectedEndpoints")
    void protectedEndpointsReturnUnauthorized(EndpointCase endpointCase) throws Exception {
        clearInvocations(authorizationService);

        mockMvc.perform(endpointCase.request().get())
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Unauthorized"));

        verify(authorizationService).authorize(anyString());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("publicEndpoints")
    void publicEndpointsDoNotRequireAuthorization(EndpointCase endpointCase) throws Exception {
        clearInvocations(authorizationService);

        mockMvc.perform(endpointCase.request().get())
                .andExpect(status().isOk());

        verify(authorizationService, never()).authorize(anyString());
    }

    private static Stream<EndpointCase> protectedEndpoints() {
        return Stream.of(
                new EndpointCase("measurement POST", () -> post("/api/v1/measurement")
                        .param("apiKey", "invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(measurementsJson())),
                new EndpointCase("measurement GET range", () -> get("/api/v1/measurement/{range}", "72h")
                        .param("apiKey", "invalid")),
                new EndpointCase("measurement GET supplier range", () -> get("/api/v1/measurement/supplier/{range}", "72h")
                        .param("apiKey", "invalid")
                        .param("stationNumber", "stationNR")),
                new EndpointCase("measurement GET supplier latest", () -> get("/api/v1/measurement/supplier/latest")
                        .param("apiKey", "invalid")
                        .param("stationNumber", "stationNR")),
                new EndpointCase("measurement GET supplier average", () -> get("/api/v1/measurement/supplier/average/{range}", "72h")
                        .param("apiKey", "invalid")
                        .param("stationNumber", "stationNR")),
                new EndpointCase("measurement GET last", () -> get("/api/v1/measurement/last/{uuid}", ID)
                        .param("apiKey", "invalid")),
                new EndpointCase("telemetry POST", () -> post("/api/v1/telemetry")
                        .param("apiKey", "invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(telemetryJson())),
                new EndpointCase("telemetry GET range", () -> get("/api/v1/telemetry/{range}", "72h")
                        .param("apiKey", "invalid")),
                new EndpointCase("telemetry GET last", () -> get("/api/v1/telemetry/last/{uuid}", ID)
                        .param("apiKey", "invalid")),
                new EndpointCase("supplier POST", () -> post("/api/v1/supplier")
                        .param("apiKey", "invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(supplierJson())),
                new EndpointCase("supplier PUT", () -> put("/api/v1/supplier")
                        .param("apiKey", "invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(supplierJson())),
                new EndpointCase("taker POST", () -> post("/api/v1/taker")
                        .param("apiKey", "invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(takerJson()))
        );
    }

    private static Stream<EndpointCase> publicEndpoints() {
        return Stream.of(
                new EndpointCase("token create", () -> post("/api/v1/token")),
                new EndpointCase("token refresh", () -> put("/api/v1/token")
                        .param("apiKey", "any")
                        .param("uuid", ID.toString())),
                new EndpointCase("token invalidate", () -> delete("/api/v1/token")
                        .param("apiKey", "any")
                        .param("uuid", ID.toString())),
                new EndpointCase("token admin get", () -> get("/api/v1/token/admin")),
                new EndpointCase("token admin activate", () -> put("/api/v1/token/admin")
                        .param("uuid", ID.toString())),
                new EndpointCase("connector post", () -> post("/api/v1/connector")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(connectorJson())),
                new EndpointCase("connector get one", () -> get("/api/v1/connector/{uuid}", ID)),
                new EndpointCase("connector get all", () -> get("/api/v1/connector")),
                new EndpointCase("connector delete", () -> delete("/api/v1/connector/{uuid}", ID)),
                new EndpointCase("contact post", () -> post("/api/v1/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contactJson())),
                new EndpointCase("contact get one", () -> get("/api/v1/contact/{uuid}", ID)),
                new EndpointCase("contact get all", () -> get("/api/v1/contact")),
                new EndpointCase("contact delete", () -> delete("/api/v1/contact/{uuid}", ID)),
                new EndpointCase("station manufacturer post", () -> post("/api/v1/stationManufacturer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(stationManufacturerJson())),
                new EndpointCase("station manufacturer get one", () -> get("/api/v1/stationManufacturer/{uuid}", ID)),
                new EndpointCase("station manufacturer get all", () -> get("/api/v1/stationManufacturer")),
                new EndpointCase("station manufacturer delete", () -> delete("/api/v1/stationManufacturer/{uuid}", ID)),
                new EndpointCase("taker service manufacturer post", () -> post("/api/v1/takerServiceManufacturer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(takerServiceManufacturerJson())),
                new EndpointCase("taker service manufacturer get one", () -> get("/api/v1/takerServiceManufacturer/{uuid}", ID)),
                new EndpointCase("taker service manufacturer get all", () -> get("/api/v1/takerServiceManufacturer")),
                new EndpointCase("taker service manufacturer delete", () -> delete("/api/v1/takerServiceManufacturer/{uuid}", ID)),
                new EndpointCase("supplier get one", () -> get("/api/v1/supplier/{uuid}", ID)),
                new EndpointCase("supplier get all", () -> get("/api/v1/supplier")),
                new EndpointCase("supplier delete", () -> delete("/api/v1/supplier/{uuid}", ID)),
                new EndpointCase("supplier connector id", () -> get("/api/v1/supplier/connectorID/{uuid}", ID)),
                new EndpointCase("taker get one", () -> get("/api/v1/taker/{uuid}", ID)),
                new EndpointCase("taker get all", () -> get("/api/v1/taker")),
                new EndpointCase("taker delete", () -> delete("/api/v1/taker/{uuid}", ID)),
                new EndpointCase("measurement system time", () -> get("/api/v1/measurement/systemTime"))
        );
    }

    private static String contactJson() {
        return """
                {
                  "organization": "org1",
                  "contactPerson": "Hans Maier",
                  "contactStreet": "Blumenweg 22",
                  "contactPlz": "1549",
                  "location": "Wien",
                  "contactCountry": "AT",
                  "emergencyNumber": "123456789",
                  "emergencyNumberTwo": "123456780",
                  "emergencyMail": "emergency@mail.com",
                  "serviceNumber": "123456789",
                  "serviceNumberTwo": "123456780",
                  "serviceMail": "service@mail.com",
                  "administrationPhoneNumber": "123456789",
                  "administrationPhoneNumberTwo": "123456780",
                  "administrationMail": "service@mail.com",
                  "contactNodes": "notes"
                }
                """;
    }

    private static String connectorJson() {
        return """
                {
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
                  "notes": "notes",
                  "apiToken": "11111111-1111-1111-1111-111111111111"
                }
                """;
    }

    private static String stationManufacturerJson() {
        return """
                {
                  "stationManufacturerName": "name",
                  "stationManufacturerType": "type",
                  "stationManufacturerFirmwareVersion": "1.0.0",
                  "stationRemark": "remarks"
                }
                """;
    }

    private static String takerServiceManufacturerJson() {
        return """
                {
                  "takerManufacturerName": "name",
                  "takerSystemName": "name",
                  "stationManufacturerFirmwareVersion": "1.0.0",
                  "requestRemark": "remarks"
                }
                """;
    }

    private static String measurementsJson() {
        return """
                {
                  "measurements": [
                    {
                      "timestamp": "2026-04-25T10:15:30",
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
                  "timestamp": "2010-10-12T08:50:00",
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
                    "notes": "notes",
                    "apiToken": "11111111-1111-1111-1111-111111111111"
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
                    "notes": "notes",
                    "apiToken": "11111111-1111-1111-1111-111111111111"
                  },
                  "refreshRate": 100
                }
                """;
    }

    private record EndpointCase(String name, Supplier<MockHttpServletRequestBuilder> request) {
        @Override
        public String toString() {
            return name;
        }
    }
}
