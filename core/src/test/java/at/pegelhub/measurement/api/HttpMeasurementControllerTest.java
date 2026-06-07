package at.pegelhub.measurement.api;

import at.pegelhub.measurement.application.MeasurementService;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
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

    private static final TimeSeriesId TIME_SERIES_ID = MEASUREMENT.timeSeriesId();

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
    void findMeasurementForTimeSeriesInRangeReturnsJsonArray() throws Exception {
        when(measurementService.getByTimeSeriesAndRange(TIME_SERIES_ID, "72h")).thenReturn(MEASUREMENTS);

        mockMvc.perform(get("/api/v1/measurement/time-series/{timeSeriesId}/{range}", TIME_SERIES_ID.value(), "72h"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].timeSeriesId.value").value(MEASUREMENT.timeSeriesId().value().toString()));
    }

    @Test
    void findLatestMeasurementByTimeSeriesReturnsJson() throws Exception {
        when(measurementService.getLatestByTimeSeries(TIME_SERIES_ID)).thenReturn(MEASUREMENT);

        mockMvc.perform(get("/api/v1/measurement/time-series/{timeSeriesId}/latest", TIME_SERIES_ID.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeSeriesId.value").value(MEASUREMENT.timeSeriesId().value().toString()));
    }

    @Test
    void findAverageMeasurementForTimeSeriesInRangeReturnsJson() throws Exception {
        when(measurementService.getAverageByTimeSeriesAndRange(TIME_SERIES_ID, "72h")).thenReturn(MEASUREMENT);

        mockMvc.perform(get("/api/v1/measurement/time-series/{timeSeriesId}/average/{range}", TIME_SERIES_ID.value(), "72h"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeSeriesId.value").value(MEASUREMENT.timeSeriesId().value().toString()));
    }

    @Test
    void getSystemTimeIsPublic() throws Exception {
        Instant ts = Instant.parse("2026-01-02T03:04:05Z");
        when(measurementService.getSystemTime()).thenReturn(ts);

        mockMvc.perform(get("/api/v1/measurement/systemTime"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("2026-01-02")));

        verify(measurementService).getSystemTime();
    }

    @Test
    void findMeasurementForTimeSeriesInRangeMapsRuntimeExceptionTo500() throws Exception {
        doThrow(new RuntimeException("range parse failed"))
                .when(measurementService)
                .getByTimeSeriesAndRange(TIME_SERIES_ID, "invalid");

        mockMvc.perform(get("/api/v1/measurement/time-series/{timeSeriesId}/{range}", TIME_SERIES_ID.value(), "invalid"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("range parse failed"));
    }

    private static String validMeasurementsPayload() {
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
}
