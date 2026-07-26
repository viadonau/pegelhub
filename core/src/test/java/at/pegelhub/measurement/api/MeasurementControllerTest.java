package at.pegelhub.measurement.api;

import at.pegelhub.measurement.api.read.MeasurementReadQueryResolver;
import at.pegelhub.measurement.application.MeasurementBucketList;
import at.pegelhub.measurement.application.MeasurementBucketResolutionPolicy;
import at.pegelhub.measurement.application.MeasurementList;
import at.pegelhub.measurement.application.MeasurementService;
import at.pegelhub.measurement.domain.MeasurementBucket;
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
@Import({MeasurementReadQueryResolver.class, MeasurementBucketResolutionPolicy.class})
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
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."));
    }

    @Test
    void listMeasurementsReturnsLeanEnvelope() throws Exception {
        when(measurementService.listMeasurements(any())).thenAnswer(invocation ->
                new MeasurementList(invocation.getArgument(0), false, MEASUREMENT_READ_ROWS));

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
                new MeasurementList(invocation.getArgument(0), false, List.of()));

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
                new MeasurementList(invocation.getArgument(0), true, List.of(MEASUREMENT_READ_ROW)));

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
    void listMeasurementBucketsReturnsChartReadyEnvelope() throws Exception {
        when(measurementService.listMeasurementBuckets(any())).thenAnswer(invocation ->
                new MeasurementBucketList(invocation.getArgument(0), List.of(new MeasurementBucket(
                        TIME_SERIES_ID,
                        Instant.parse("2010-10-12T08:00:00Z"),
                        Instant.parse("2010-10-12T08:05:00Z"),
                        1.0,
                        3))));

        mockMvc.perform(get("/api/v1/time-series/{timeSeriesId}/measurements/buckets", TIME_SERIES_ID.value())
                        .param("last", "24h")
                        .param("bucket", "5m"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeSeriesId").value(TIME_SERIES_ID.value().toString()))
                .andExpect(jsonPath("$.window.requested").value("24h"))
                .andExpect(jsonPath("$.resolution.bucket").value("5m"))
                .andExpect(jsonPath("$.resolution.aggregation").value("average"))
                .andExpect(jsonPath("$.resolution.maxPoints").value(nullValue()))
                .andExpect(jsonPath("$.points[0].from").value("2010-10-12T08:00:00Z"))
                .andExpect(jsonPath("$.points[0].to").value("2010-10-12T08:05:00Z"))
                .andExpect(jsonPath("$.points[0].value").value(1.0))
                .andExpect(jsonPath("$.points[0].sampleCount").value(3))
                .andExpect(jsonPath("$.points[0].timeSeriesId").doesNotExist())
                .andExpect(jsonPath("$.points[0].observedAt").doesNotExist())
                .andExpect(jsonPath("$.points[0].submittedByConnectorId").doesNotExist());
    }

    @Test
    void listMeasurementBucketsDerivesBucketFromMaxPoints() throws Exception {
        when(measurementService.listMeasurementBuckets(any())).thenAnswer(invocation ->
                new MeasurementBucketList(invocation.getArgument(0), List.of()));

        mockMvc.perform(get("/api/v1/time-series/{timeSeriesId}/measurements/buckets", TIME_SERIES_ID.value())
                        .param("last", "24h")
                        .param("maxPoints", "240"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolution.bucket").value("15m"))
                .andExpect(jsonPath("$.resolution.maxPoints").value(240))
                .andExpect(jsonPath("$.points").isEmpty());

        verify(measurementService).listMeasurementBuckets(argThat(query ->
                TIME_SERIES_ID.equals(query.timeSeriesId())
                        && "24h".equals(query.window().requested())
                        && "15m".equals(query.resolution().bucketWidth().toString())
                        && Integer.valueOf(240).equals(query.resolution().targetPointCount())));
    }

    @Test
    void listMeasurementBucketsRejectsMixedResolutionModes() throws Exception {
        mockMvc.perform(get("/api/v1/time-series/{timeSeriesId}/measurements/buckets", TIME_SERIES_ID.value())
                        .param("last", "24h")
                        .param("bucket", "5m")
                        .param("maxPoints", "240"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.detail").value(containsString("Provide either bucket or maxPoints")));
    }

    @Test
    void listMeasurementsRejectsMissingWindow() throws Exception {
        mockMvc.perform(get("/api/v1/time-series/{timeSeriesId}/measurements", TIME_SERIES_ID.value()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.detail").value(containsString("Provide either last or from/to")));
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
