package at.pegelhub.measurement.api;

import at.pegelhub.measurement.application.MeasurementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import static at.pegelhub.testsupport.ExampleData.ID;
import static at.pegelhub.testsupport.ExampleData.MEASUREMENT;
import static at.pegelhub.testsupport.ExampleData.MEASUREMENTS;
import static org.mockito.ArgumentMatchers.any;
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
    private MeasurementService measurementService;

    @Test
    void writeMeasurementDataDelegatesToService() throws Exception {
        mockMvc.perform(post("/api/v1/measurement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validMeasurementsPayload()))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
        verify(measurementService).writeMeasurements(any());
    }

    @Test
    void writeMeasurementDataMapsServiceExceptionTo500() throws Exception {
        doThrow(new RuntimeException("write failed")).when(measurementService).writeMeasurements(any());

        mockMvc.perform(post("/api/v1/measurement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validMeasurementsPayload()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("write failed"));
    }

    @Test
    void findMeasurementInRangeReturnsJsonArray() throws Exception {
        when(measurementService.getByRange("72h")).thenReturn(MEASUREMENTS);

        mockMvc.perform(get("/api/v1/measurement/{range}", "72h"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].measurement").value(MEASUREMENT.measurement().toString()));
    }

    @Test
    void findMeasurementForSupplierInRangeReturnsJsonArray() throws Exception {
        when(measurementService.getBySupplierAndRange("stationNR", "72h")).thenReturn(MEASUREMENTS);

        mockMvc.perform(get("/api/v1/measurement/supplier/{range}", "72h")
                        .param("stationNumber", "stationNR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].measurement").value(MEASUREMENT.measurement().toString()));
    }

    @Test
    void findLatestMeasurementBySupplierReturnsJson() throws Exception {
        when(measurementService.getLatestBySupplier("stationNR")).thenReturn(MEASUREMENT);

        mockMvc.perform(get("/api/v1/measurement/supplier/latest")
                        .param("stationNumber", "stationNR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurement").value(MEASUREMENT.measurement().toString()));
    }

    @Test
    void findAverageMeasurementForSupplierInRangeReturnsJson() throws Exception {
        when(measurementService.getAverageBySupplierAndRange("stationNR", "72h")).thenReturn(MEASUREMENT);

        mockMvc.perform(get("/api/v1/measurement/supplier/average/{range}", "72h")
                        .param("stationNumber", "stationNR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurement").value(MEASUREMENT.measurement().toString()));
    }

    @Test
    void findMeasurementByIdReturnsJson() throws Exception {
        when(measurementService.getLastData(ID)).thenReturn(MEASUREMENT);

        mockMvc.perform(get("/api/v1/measurement/last/{uuid}", ID))
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
    void averageEndpointRequiresStationNumber() throws Exception {
        mockMvc.perform(get("/api/v1/measurement/supplier/average/{range}", "72h"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findMeasurementForSupplierInRangeMapsRuntimeExceptionTo500() throws Exception {
        doThrow(new RuntimeException("range parse failed"))
                .when(measurementService)
                .getBySupplierAndRange("stationNR", "invalid");

        mockMvc.perform(get("/api/v1/measurement/supplier/{range}", "invalid")
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
