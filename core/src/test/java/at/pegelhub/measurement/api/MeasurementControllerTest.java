package at.pegelhub.measurement.api;

import at.pegelhub.measurement.api.read.MeasurementReadQueryResolver;
import at.pegelhub.measurement.application.MeasurementList;
import at.pegelhub.measurement.application.MeasurementIntervalList;
import at.pegelhub.measurement.application.MeasurementInterval;
import at.pegelhub.measurement.application.MeasurementService;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static at.pegelhub.testsupport.ExampleData.MEASUREMENT;
import static at.pegelhub.testsupport.ExampleData.MEASUREMENT_READ_ROW;
import static at.pegelhub.testsupport.ExampleData.MEASUREMENT_READ_ROWS;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MeasurementController.class)
@Import(MeasurementReadQueryResolver.class)
class MeasurementControllerTest {

    private static final TimeSeriesId TIME_SERIES_ID = MEASUREMENT.timeSeriesId();
    private static final Instant NOW = Instant.parse("2026-06-17T13:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MeasurementService measurementService;

    @MockitoBean
    private Clock clock;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(NOW);
    }

    @Test
    void writeMeasurementDataDelegatesToService() throws Exception {
        mockMvc.perform(post("/api/v1/measurements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validMeasurementsPayload()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(measurementService).writeMeasurements(any());
    }

    @Test
    void writeMeasurementDataMapsServiceExceptionTo500() throws Exception {
        doThrow(new RuntimeException("write failed")).when(measurementService).writeMeasurements(any());

        mockMvc.perform(post("/api/v1/measurements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validMeasurementsPayload()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("write failed"));
    }

    @Test
    void listMeasurementsReturnsLeanEnvelope() throws Exception {
        when(measurementService.listMeasurements(any())).thenAnswer(invocation ->
                new MeasurementList(invocation.getArgument(0), false, MEASUREMENT_READ_ROWS, "cm"));

        mockMvc.perform(get("/api/v1/time-series/{timeSeriesId}/measurements", TIME_SERIES_ID.value())
                        .param("from", "2010-10-12T08:00:00Z")
                        .param("to", "2010-10-12T09:00:00Z")
                        .param("limit", "100")
                        .param("order", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeSeriesId").value(TIME_SERIES_ID.value().toString()))
                .andExpect(jsonPath("$.window.from").value("2010-10-12T08:00:00Z"))
                .andExpect(jsonPath("$.window.to").value("2010-10-12T09:00:00Z"))
                .andExpect(jsonPath("$.order").value("asc"))
                .andExpect(jsonPath("$.limit").value(100))
                .andExpect(jsonPath("$.truncated").value(false))
                .andExpect(jsonPath("$.unit").value("cm"))
                .andExpect(jsonPath("$.representation").value("canonical"))
                .andExpect(jsonPath("$.measurements[0].observedAt").value(MEASUREMENT_READ_ROW.observedAt().toString()))
                .andExpect(jsonPath("$.measurements[0].value").value(MEASUREMENT_READ_ROW.value()))
                .andExpect(jsonPath("$.measurements[0].receivedAt").doesNotExist())
                .andExpect(jsonPath("$.measurements[0].submittedByConnectorId").doesNotExist())
                .andExpect(jsonPath("$.measurements[0].timeSeriesId").doesNotExist());

        verify(measurementService).listMeasurements(argThat(query ->
                query.timeSeriesId().equals(TIME_SERIES_ID)
                        && query.limit() == 100
                        && query.window().requested() == null));
    }

    @Test
    void listMeasurementsSupportsRelativeWindow() throws Exception {
        when(measurementService.listMeasurements(any())).thenAnswer(invocation ->
                new MeasurementList(invocation.getArgument(0), false, List.of(), "cm"));

        mockMvc.perform(get("/api/v1/time-series/{timeSeriesId}/measurements", TIME_SERIES_ID.value())
                        .param("last", "24h"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.window.from").value("2026-06-16T13:00:00Z"))
                .andExpect(jsonPath("$.window.to").value("2026-06-17T13:00:00Z"))
                .andExpect(jsonPath("$.window.requested").value("24h"));
    }

    @Test
    void listMeasurementsReportsTruncation() throws Exception {
        when(measurementService.listMeasurements(any())).thenAnswer(invocation ->
                new MeasurementList(invocation.getArgument(0), true, List.of(MEASUREMENT_READ_ROW), "cm"));

        mockMvc.perform(get("/api/v1/time-series/{timeSeriesId}/measurements", TIME_SERIES_ID.value())
                        .param("last", "24h")
                        .param("order", "desc")
                        .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order").value("desc"))
                .andExpect(jsonPath("$.limit").value(1))
                .andExpect(jsonPath("$.truncated").value(true));

        verify(measurementService).listMeasurements(argThat(query ->
                query.order() == at.pegelhub.measurement.application.MeasurementOrder.DESC
                        && query.limit() == 1));
    }

    @Test
    void intervalApiReturnsExplicitMethodUnitAndEmptyEvidence() throws Exception {
        when(measurementService.listMeasurementIntervals(any())).thenAnswer(invocation ->
                new MeasurementIntervalList(invocation.getArgument(0), NOW, "cm", List.of(
                        new MeasurementInterval(NOW.minusSeconds(3_600), NOW, null, 0,
                                0, null, "closed", "absent"))));

        mockMvc.perform(get("/api/v1/time-series/{timeSeriesId}/measurements/intervals", TIME_SERIES_ID.value())
                        .param("from", "2026-06-17T12:00:00Z")
                        .param("to", "2026-06-17T13:00:00Z")
                .param("interval", "1h"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("time-weighted-step"))
                .andExpect(jsonPath("$.timeBasis").value("UTC"))
                .andExpect(jsonPath("$.closedOnly").value(true))
                .andExpect(jsonPath("$.unit").value("cm"))
                .andExpect(jsonPath("$.intervals[0].mean").value(nullValue()))
                .andExpect(jsonPath("$.intervals[0].observationCount").value(0))
                .andExpect(jsonPath("$.intervals[0].supportStatus").value("absent"));
    }

    @Test
    void intervalApiRejectsPartialAndInvalidWidthRequests() throws Exception {
        mockMvc.perform(get("/api/v1/time-series/{timeSeriesId}/measurements/intervals", TIME_SERIES_ID.value())
                        .param("from", "2026-06-17T12:00:00Z")
                        .param("to", "2026-06-17T13:00:00Z"))
                .andExpect(status().isBadRequest());

        for (String interval : List.of("14m", "15m")) {
            mockMvc.perform(get("/api/v1/time-series/{timeSeriesId}/measurements/intervals", TIME_SERIES_ID.value())
                            .param("from", "2026-06-17T12:00:01Z")
                            .param("to", "2026-06-17T13:00:01Z")
                            .param("interval", interval))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(get("/api/v1/time-series/{timeSeriesId}/measurements/intervals", TIME_SERIES_ID.value())
                        .param("from", "2026-06-17T00:00:00Z")
                        .param("to", Instant.parse("2026-06-17T00:00:00Z").plusSeconds(50_001L * 900).toString())
                        .param("interval", "15m"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oldBucketEndpointIsNotExposed() throws Exception {
        mockMvc.perform(get("/api/v1/time-series/{timeSeriesId}/measurements/buckets", TIME_SERIES_ID.value())
                        .param("last", "24h"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listMeasurementsRejectsMissingWindow() throws Exception {
        mockMvc.perform(get("/api/v1/time-series/{timeSeriesId}/measurements", TIME_SERIES_ID.value()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Provide either last or from/to")));
    }

    @Test
    void listMeasurementsRejectsAnOutOfRangeLimitAtTheHttpInput() throws Exception {
        mockMvc.perform(get("/api/v1/time-series/{timeSeriesId}/measurements", TIME_SERIES_ID.value())
                        .param("last", "24h")
                        .param("limit", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listMeasurementsRejectsALimitAboveTheServerMaximum() throws Exception {
        mockMvc.perform(get("/api/v1/time-series/{timeSeriesId}/measurements", TIME_SERIES_ID.value())
                        .param("last", "24h")
                        .param("limit", "10001"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void readsBindAndDescribeExplicitRepresentations() throws Exception {
        when(measurementService.listMeasurements(any())).thenAnswer(invocation ->
                new MeasurementList(invocation.getArgument(0), false, List.of(), "l/s"));

        mockMvc.perform(get("/api/v1/time-series/{timeSeriesId}/measurements", TIME_SERIES_ID.value())
                        .param("last", "24h").param("representation", "litres-per-second"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.representation").value("litres-per-second"))
                .andExpect(jsonPath("$.unit").value("l/s"));
    }

    @Test
    void unknownAndBlankRepresentationsAreNotSilentlyTreatedAsCanonical() throws Exception {
        for (String value : new String[]{"l/s", "unknown", ""}) {
            mockMvc.perform(get("/api/v1/time-series/{timeSeriesId}/measurements", TIME_SERIES_ID.value())
                            .param("last", "24h").param("representation", value))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void getSystemTimeIsPublic() throws Exception {
        Instant ts = Instant.parse("2026-01-02T03:04:05Z");
        when(measurementService.getSystemTime()).thenReturn(ts);

        mockMvc.perform(get("/api/v1/measurements/system-time"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("2026-01-02")));

        verify(measurementService).getSystemTime();
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
