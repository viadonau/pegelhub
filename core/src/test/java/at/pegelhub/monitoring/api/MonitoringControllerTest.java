package at.pegelhub.monitoring.api;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measuringpoint.domain.MeasuringPoint;
import at.pegelhub.measuringpoint.domain.BankSide;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.measurement.application.LatestMeasurement;
import at.pegelhub.monitoring.application.MonitoringCollection;
import at.pegelhub.monitoring.application.MonitoringDetail;
import at.pegelhub.monitoring.application.MonitoringQueryService;
import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.station.domain.Station;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.stationowner.domain.StationOwner;
import at.pegelhub.stationowner.domain.StationOwnerId;
import at.pegelhub.timeseries.domain.ObservedPropertyCode;
import at.pegelhub.timeseries.domain.TimeSeries;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import at.pegelhub.timeseries.domain.UnitCode;
import com.influxdb.exceptions.InfluxException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MonitoringController.class)
class MonitoringControllerTest {

    private static final UUID SERIES_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant TO = Instant.parse("2026-08-15T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MonitoringQueryService monitoring;

    @Test
    void returnsCollectionEnvelope() throws Exception {
        when(monitoring.readCollection(Duration.ofDays(7))).thenReturn(collection());

        mockMvc.perform(get("/api/v1/monitoring/time-series").param("latestWithin", "7d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(SERIES_ID.toString()))
                .andExpect(jsonPath("$.items[0].latestMeasurement.value").value(12.5))
                .andExpect(jsonPath("$.items[0].station.stationNumber").value("AT-001"));
    }

    @Test
    void returnsDetailEnvelope() throws Exception {
        when(monitoring.readDetail(any(), any())).thenReturn(detail());

        mockMvc.perform(get("/api/v1/monitoring/time-series/{id}", SERIES_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SERIES_ID.toString()))
                .andExpect(jsonPath("$.measuringPoint.referenceLevel").value(142.75))
                .andExpect(jsonPath("$.stationOwner.shortName").value("viadonau"));
    }

    @Test
    void mapsInvalidParametersTo400() throws Exception {
        mockMvc.perform(get("/api/v1/monitoring/time-series").param("latestWithin", "366d"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("latestWithin must not exceed 365d"));
    }

    @Test
    void mapsMissingSeriesTo404() throws Exception {
        when(monitoring.readDetail(any(), any())).thenThrow(new NotFoundException("missing"));

        mockMvc.perform(get("/api/v1/monitoring/time-series/{id}", SERIES_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().string("missing"));
    }

    @Test
    void mapsInfluxFailureTo503WithoutLeakingItsMessage() throws Exception {
        when(monitoring.readCollection(any())).thenThrow(new InfluxException("token=secret"));

        mockMvc.perform(get("/api/v1/monitoring/time-series"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string("Measurement store unavailable"));
    }

    private static MonitoringCollection collection() {
        return new MonitoringCollection(
                List.of(new MonitoringCollection.MonitoringTimeSeriesSummary(
                        timeSeries(), measuringPoint(), station(), new LatestMeasurement(new TimeSeriesId(SERIES_ID), TO, 12.5))));
    }

    private static MonitoringDetail detail() {
        return new MonitoringDetail(
                timeSeries(),
                measuringPoint(),
                station(),
                new StationOwner(new StationOwnerId(UUID.fromString("44444444-4444-4444-4444-444444444444")), "Owner", "viadonau", null),
                new LatestMeasurement(new TimeSeriesId(SERIES_ID), TO, 12.5));
    }

    private static TimeSeries timeSeries() {
        return new TimeSeries(
                new TimeSeriesId(SERIES_ID),
                new MeasuringPointId(UUID.fromString("22222222-2222-2222-2222-222222222222")),
                new ObservedPropertyCode("water-level"),
                new UnitCode("cm"),
                null,
                new ConnectorId(UUID.fromString("55555555-5555-5555-5555-555555555555")));
    }

    private static MeasuringPoint measuringPoint() {
        return new MeasuringPoint(
                new MeasuringPointId(UUID.fromString("22222222-2222-2222-2222-222222222222")),
                new StationId(UUID.fromString("33333333-3333-3333-3333-333333333333")),
                "Main gauge",
                142.75,
                2020,
                1933.2,
                BankSide.LEFT,
                120.0,
                280.0,
                620.0,
                760.0);
    }

    private static Station station() {
        return new Station(
                new StationId(UUID.fromString("33333333-3333-3333-3333-333333333333")),
                new StationOwnerId(UUID.fromString("44444444-4444-4444-4444-444444444444")),
                "AT-001",
                "Kienstock",
                "Danube",
                null);
    }
}
