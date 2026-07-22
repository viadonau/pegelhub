package at.pegelhub.timeseries.api;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.timeseries.application.CreateTimeSeriesCommand;
import at.pegelhub.timeseries.application.TimeSeriesService;
import at.pegelhub.timeseries.domain.ExternalTimeSeriesCode;
import at.pegelhub.timeseries.domain.ObservedPropertyCode;
import at.pegelhub.timeseries.domain.TimeSeries;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import at.pegelhub.timeseries.domain.UnitCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HttpTimeSeriesController.class)
class HttpTimeSeriesControllerTest {

    private static final UUID TIME_SERIES_ID = UUID.fromString("00d570f1-9547-40fd-9b16-30ac083d0723");
    private static final UUID MEASURING_POINT_ID = UUID.fromString("a9a3d5e7-de04-43a2-8b10-abfb1bdd2819");
    private static final UUID STATION_ID = UUID.fromString("1f69841d-455b-4ff8-adb0-fcc18481ad58");
    private static final UUID SOURCE_CONNECTOR_ID = UUID.fromString("b8614980-37c8-46b6-8d78-b6fb6f84c950");
    private static final TimeSeries TIME_SERIES = new TimeSeries(
            new TimeSeriesId(TIME_SERIES_ID),
            new MeasuringPointId(MEASURING_POINT_ID),
            new ObservedPropertyCode("water-level"),
            new UnitCode("cm"),
            new ExternalTimeSeriesCode("main-stage"),
            new ConnectorId(SOURCE_CONNECTOR_ID));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TimeSeriesService timeSeries;

    @Test
    void createsTimeSeries() throws Exception {
        when(timeSeries.create(any())).thenReturn(TIME_SERIES);

        mockMvc.perform(post("/api/v1/time-series")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "measuringPointId": "%s",
                                  "observedProperty": "water-level",
                                  "unit": "cm",
                                  "externalCode": "main-stage",
                                  "sourceConnectorId": "%s"
                                }
                                """.formatted(MEASURING_POINT_ID, SOURCE_CONNECTOR_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(TIME_SERIES_ID.toString()))
                .andExpect(jsonPath("$.measuringPointId").value(MEASURING_POINT_ID.toString()))
                .andExpect(jsonPath("$.observedProperty").value("water-level"))
                .andExpect(jsonPath("$.unit").value("cm"))
                .andExpect(jsonPath("$.externalCode").value("main-stage"))
                .andExpect(jsonPath("$.sourceConnectorId").value(SOURCE_CONNECTOR_ID.toString()))
                .andExpect(jsonPath("$.stationId").doesNotExist())
                .andExpect(jsonPath("$.referenceLevel").doesNotExist());

        verify(timeSeries).create(eq(new CreateTimeSeriesCommand(
                new MeasuringPointId(MEASURING_POINT_ID),
                new ObservedPropertyCode("water-level"),
                new UnitCode("cm"),
                new ExternalTimeSeriesCode("main-stage"),
                new ConnectorId(SOURCE_CONNECTOR_ID))));
    }

    @Test
    void rejectsCreateWithoutMeasuringPointId() throws Exception {
        mockMvc.perform(post("/api/v1/time-series")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "observedProperty": "water-level",
                                  "unit": "cm"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getsTimeSeries() throws Exception {
        when(timeSeries.get(new TimeSeriesId(TIME_SERIES_ID))).thenReturn(TIME_SERIES);

        mockMvc.perform(get("/api/v1/time-series/{id}", TIME_SERIES_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TIME_SERIES_ID.toString()))
                .andExpect(jsonPath("$.measuringPointId").value(MEASURING_POINT_ID.toString()))
                .andExpect(jsonPath("$.observedProperty").value("water-level"));
    }

    @Test
    void listsTimeSeries() throws Exception {
        when(timeSeries.list()).thenReturn(List.of(TIME_SERIES));

        mockMvc.perform(get("/api/v1/time-series"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(TIME_SERIES_ID.toString()));
    }

    @Test
    void listsTimeSeriesForMeasuringPoint() throws Exception {
        when(timeSeries.listForMeasuringPoint(new MeasuringPointId(MEASURING_POINT_ID)))
                .thenReturn(List.of(TIME_SERIES));

        mockMvc.perform(get("/api/v1/time-series")
                        .param("measuringPointId", MEASURING_POINT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].measuringPointId").value(MEASURING_POINT_ID.toString()));
    }

    @Test
    void listsTimeSeriesForStation() throws Exception {
        when(timeSeries.listForStation(new StationId(STATION_ID))).thenReturn(List.of(TIME_SERIES));

        mockMvc.perform(get("/api/v1/time-series")
                        .param("stationId", STATION_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].measuringPointId").value(MEASURING_POINT_ID.toString()));
    }

    @Test
    void rejectsCombinedFilters() throws Exception {
        mockMvc.perform(get("/api/v1/time-series")
                        .param("measuringPointId", MEASURING_POINT_ID.toString())
                        .param("stationId", STATION_ID.toString()))
                .andExpect(status().isBadRequest());
    }
}
