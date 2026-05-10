package at.pegelhub.measurement.api;

import at.pegelhub.auth.application.AuthTokenIdHolder;
import at.pegelhub.auth.application.AuthorizationService;
import at.pegelhub.measurement.application.MeasurementService;
import at.pegelhub.shared.error.UnauthorizedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import static at.pegelhub.testsupport.ExampleData.ID;
import static at.pegelhub.testsupport.ExampleData.MEASUREMENT;
import static at.pegelhub.testsupport.ExampleData.MEASUREMENTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

@WebMvcTest(HttpMeasurementController.class)
class HttpMeasurementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthorizationService authorizationService;

    @MockitoBean
    private MeasurementService measurementService;

    @AfterEach
    void clearAuthHolder() {
        AuthTokenIdHolder.clear();
    }

    @Test
    void writeMeasurementDataUsesAuthContextAndClearsHolderAfterSuccess() throws Exception {
        UUID tokenId = UUID.randomUUID();
        when(authorizationService.authorize("valid")).thenReturn(tokenId);
        doAnswer(invocation -> {
            assertEquals(tokenId, AuthTokenIdHolder.get());
            return null;
        }).when(measurementService).writeMeasurements(any());

        mockMvc.perform(post("/api/v1/measurement")
                        .param("apiKey", "valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validMeasurementsPayload()))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        assertNull(AuthTokenIdHolder.get());
    }

    @Test
    void writeMeasurementDataClearsHolderAfterException() throws Exception {
        UUID tokenId = UUID.randomUUID();
        when(authorizationService.authorize("valid")).thenReturn(tokenId);
        doAnswer(invocation -> {
            assertEquals(tokenId, AuthTokenIdHolder.get());
            throw new RuntimeException("write failed");
        }).when(measurementService).writeMeasurements(any());

        mockMvc.perform(post("/api/v1/measurement")
                        .param("apiKey", "valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validMeasurementsPayload()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("write failed"));

        assertNull(AuthTokenIdHolder.get());
    }

    @Test
    void findMeasurementInRangeReturnsJsonArray() throws Exception {
        when(authorizationService.authorize("valid")).thenReturn(UUID.randomUUID());
        when(measurementService.getByRange("72h")).thenReturn(MEASUREMENTS);

        mockMvc.perform(get("/api/v1/measurement/{range}", "72h")
                        .param("apiKey", "valid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].measurement").value(MEASUREMENT.measurement().toString()));
    }

    @Test
    void findMeasurementForSupplierInRangeReturnsJsonArray() throws Exception {
        when(authorizationService.authorize("valid")).thenReturn(UUID.randomUUID());
        when(measurementService.getBySupplierAndRange("stationNR", "72h")).thenReturn(MEASUREMENTS);

        mockMvc.perform(get("/api/v1/measurement/supplier/{range}", "72h")
                        .param("apiKey", "valid")
                        .param("stationNumber", "stationNR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].measurement").value(MEASUREMENT.measurement().toString()));
    }

    @Test
    void findLatestMeasurementBySupplierReturnsJson() throws Exception {
        when(authorizationService.authorize("valid")).thenReturn(UUID.randomUUID());
        when(measurementService.getLatestBySupplier("stationNR")).thenReturn(MEASUREMENT);

        mockMvc.perform(get("/api/v1/measurement/supplier/latest")
                        .param("apiKey", "valid")
                        .param("stationNumber", "stationNR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurement").value(MEASUREMENT.measurement().toString()));
    }

    @Test
    void findAverageMeasurementForSupplierInRangeReturnsJson() throws Exception {
        when(authorizationService.authorize("valid")).thenReturn(UUID.randomUUID());
        when(measurementService.getAverageBySupplierAndRange("stationNR", "72h")).thenReturn(MEASUREMENT);

        mockMvc.perform(get("/api/v1/measurement/supplier/average/{range}", "72h")
                        .param("apiKey", "valid")
                        .param("stationNumber", "stationNR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurement").value(MEASUREMENT.measurement().toString()));
    }

    @Test
    void findMeasurementByIdReturnsJson() throws Exception {
        when(authorizationService.authorize("valid")).thenReturn(UUID.randomUUID());
        when(measurementService.getLastData(ID)).thenReturn(MEASUREMENT);

        mockMvc.perform(get("/api/v1/measurement/last/{uuid}", ID)
                        .param("apiKey", "valid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurement").value(MEASUREMENT.measurement().toString()));
    }

    @Test
    void getSystemTimeIsPublic() throws Exception {
        Timestamp ts = Timestamp.valueOf(LocalDateTime.of(2026, 1, 2, 3, 4, 5));
        when(measurementService.getSystemTime()).thenReturn(ts);

        mockMvc.perform(get("/api/v1/measurement/systemTime"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("2026-01-02")));

        verify(measurementService).getSystemTime();
    }

    @Test
    void protectedMeasurementEndpointsRejectUnauthorizedRequests() throws Exception {
        doThrow(new UnauthorizedException("unauthorized")).when(authorizationService).authorize(any());

        mockMvc.perform(post("/api/v1/measurement")
                        .param("apiKey", "invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validMeasurementsPayload()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Unauthorized"));

        mockMvc.perform(get("/api/v1/measurement/{range}", "72h").param("apiKey", "invalid"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/measurement/supplier/{range}", "72h")
                        .param("apiKey", "invalid")
                        .param("stationNumber", "stationNR"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/measurement/supplier/latest")
                        .param("apiKey", "invalid")
                        .param("stationNumber", "stationNR"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/measurement/supplier/average/{range}", "72h")
                        .param("apiKey", "invalid")
                        .param("stationNumber", "stationNR"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/measurement/last/{uuid}", ID)
                        .param("apiKey", "invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void averageEndpointRequiresStationNumber() throws Exception {
        when(authorizationService.authorize("valid")).thenReturn(UUID.randomUUID());

        mockMvc.perform(get("/api/v1/measurement/supplier/average/{range}", "72h")
                        .param("apiKey", "valid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findMeasurementForSupplierInRangeMapsRuntimeExceptionTo500() throws Exception {
        when(authorizationService.authorize("valid")).thenReturn(UUID.randomUUID());
        doThrow(new RuntimeException("range parse failed"))
                .when(measurementService)
                .getBySupplierAndRange("stationNR", "invalid");

        mockMvc.perform(get("/api/v1/measurement/supplier/{range}", "invalid")
                        .param("apiKey", "valid")
                        .param("stationNumber", "stationNR"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("range parse failed"));
    }

    private static String validMeasurementsPayload() {
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
}
