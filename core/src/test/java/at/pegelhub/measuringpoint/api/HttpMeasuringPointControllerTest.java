package at.pegelhub.measuringpoint.api;

import at.pegelhub.measuringpoint.application.CreateMeasuringPointCommand;
import at.pegelhub.measuringpoint.application.MeasuringPointService;
import at.pegelhub.measuringpoint.domain.MeasuringPoint;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.station.domain.StationId;
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

@WebMvcTest(HttpMeasuringPointController.class)
class HttpMeasuringPointControllerTest {

    private static final UUID MEASURING_POINT_ID = UUID.fromString("00d570f1-9547-40fd-9b16-30ac083d0723");
    private static final UUID STATION_ID = UUID.fromString("a9a3d5e7-de04-43a2-8b10-abfb1bdd2819");
    private static final MeasuringPoint MEASURING_POINT = new MeasuringPoint(
            new MeasuringPointId(MEASURING_POINT_ID),
            new StationId(STATION_ID),
            "Main gauge",
            120.0,
            2010,
            1921.34,
            "R",
            162.0,
            295.0,
            480.0,
            760.0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MeasuringPointService measuringPoints;

    @Test
    void createsMeasuringPoint() throws Exception {
        when(measuringPoints.create(any())).thenReturn(MEASURING_POINT);

        mockMvc.perform(post("/api/v1/measuring-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stationId": "%s",
                                  "name": "Main gauge",
                                  "referenceLevel": 120.0,
                                  "referenceYear": 2010,
                                  "riverKilometer": 1921.34,
                                  "bank": "R",
                                  "rnw": 162.0,
                                  "mw": 295.0,
                                  "hsw": 480.0,
                                  "hw100": 760.0
                                }
                                """.formatted(STATION_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(MEASURING_POINT_ID.toString()))
                .andExpect(jsonPath("$.stationId").value(STATION_ID.toString()))
                .andExpect(jsonPath("$.name").value("Main gauge"))
                .andExpect(jsonPath("$.referenceLevel").value(120.0))
                .andExpect(jsonPath("$.referenceYear").value(2010))
                .andExpect(jsonPath("$.riverKilometer").value(1921.34))
                .andExpect(jsonPath("$.bank").value("R"))
                .andExpect(jsonPath("$.rnw").value(162.0))
                .andExpect(jsonPath("$.mw").value(295.0))
                .andExpect(jsonPath("$.hsw").value(480.0))
                .andExpect(jsonPath("$.hw100").value(760.0));

        verify(measuringPoints).create(eq(new CreateMeasuringPointCommand(
                new StationId(STATION_ID),
                "Main gauge",
                120.0,
                2010,
                1921.34,
                "R",
                162.0,
                295.0,
                480.0,
                760.0)));
    }

    @Test
    void rejectsCreateWithoutStationId() throws Exception {
        mockMvc.perform(post("/api/v1/measuring-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Main gauge"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsCreateWithBlankName() throws Exception {
        mockMvc.perform(post("/api/v1/measuring-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stationId": "%s",
                                  "name": " "
                                }
                                """.formatted(STATION_ID)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsCreateWithOverlongName() throws Exception {
        mockMvc.perform(post("/api/v1/measuring-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stationId": "%s",
                                  "name": "%s"
                                }
                                """.formatted(STATION_ID, "x".repeat(201))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsCreateWithOverlongBank() throws Exception {
        mockMvc.perform(post("/api/v1/measuring-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stationId": "%s",
                                  "name": "Main gauge",
                                  "bank": "%s"
                                }
                                """.formatted(STATION_ID, "x".repeat(41))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsCreateWithInvalidReferenceYear() throws Exception {
        mockMvc.perform(post("/api/v1/measuring-points")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stationId": "%s",
                                  "name": "Main gauge",
                                  "referenceYear": 10000
                                }
                                """.formatted(STATION_ID)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getsMeasuringPoint() throws Exception {
        when(measuringPoints.get(new MeasuringPointId(MEASURING_POINT_ID))).thenReturn(MEASURING_POINT);

        mockMvc.perform(get("/api/v1/measuring-points/{id}", MEASURING_POINT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(MEASURING_POINT_ID.toString()))
                .andExpect(jsonPath("$.stationId").value(STATION_ID.toString()));
    }

    @Test
    void listsMeasuringPoints() throws Exception {
        when(measuringPoints.list()).thenReturn(List.of(MEASURING_POINT));

        mockMvc.perform(get("/api/v1/measuring-points"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(MEASURING_POINT_ID.toString()));
    }

    @Test
    void listsMeasuringPointsForStation() throws Exception {
        when(measuringPoints.listForStation(new StationId(STATION_ID))).thenReturn(List.of(MEASURING_POINT));

        mockMvc.perform(get("/api/v1/measuring-points")
                        .param("stationId", STATION_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stationId").value(STATION_ID.toString()));
    }
}
