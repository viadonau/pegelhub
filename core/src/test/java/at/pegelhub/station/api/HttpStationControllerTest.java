package at.pegelhub.station.api;

import at.pegelhub.station.application.CreateStationCommand;
import at.pegelhub.station.application.StationService;
import at.pegelhub.station.domain.Station;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.stationowner.domain.StationOwnerId;
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

@WebMvcTest(HttpStationController.class)
class HttpStationControllerTest {

    private static final UUID STATION_ID = UUID.fromString("dfcbcbba-9543-4a19-84dc-e9b9f6b948ef");
    private static final UUID OWNER_ID = UUID.fromString("18908c6b-d92c-4eff-bfe7-206a973da841");
    private static final Station STATION = new Station(
            new StationId(STATION_ID),
            new StationOwnerId(OWNER_ID),
            "1001",
            "Kienstock",
            "Danube",
            "Wachau");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StationService stations;

    @Test
    void createsStation() throws Exception {
        when(stations.create(any())).thenReturn(STATION);

        mockMvc.perform(post("/api/v1/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ownerId": "%s",
                                  "stationNumber": "1001",
                                  "name": "Kienstock",
                                  "waterBody": "Danube",
                                  "location": "Wachau"
                                }
                                """.formatted(OWNER_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(STATION_ID.toString()))
                .andExpect(jsonPath("$.ownerId").value(OWNER_ID.toString()))
                .andExpect(jsonPath("$.stationNumber").value("1001"))
                .andExpect(jsonPath("$.name").value("Kienstock"))
                .andExpect(jsonPath("$.waterBody").value("Danube"))
                .andExpect(jsonPath("$.location").value("Wachau"));

        verify(stations).create(eq(new CreateStationCommand(
                new StationOwnerId(OWNER_ID),
                "1001",
                "Kienstock",
                "Danube",
                "Wachau")));
    }

    @Test
    void rejectsCreateWithoutOwnerId() throws Exception {
        mockMvc.perform(post("/api/v1/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stationNumber": "1001",
                                  "name": "Kienstock",
                                  "waterBody": "Danube"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsCreateWithoutWaterBody() throws Exception {
        mockMvc.perform(post("/api/v1/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ownerId": "%s",
                                  "stationNumber": "1001",
                                  "name": "Kienstock"
                                }
                                """.formatted(OWNER_ID)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getsStation() throws Exception {
        when(stations.get(new StationId(STATION_ID))).thenReturn(STATION);

        mockMvc.perform(get("/api/v1/stations/{id}", STATION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(STATION_ID.toString()))
                .andExpect(jsonPath("$.ownerId").value(OWNER_ID.toString()))
                .andExpect(jsonPath("$.name").value("Kienstock"));
    }

    @Test
    void listsStations() throws Exception {
        when(stations.list()).thenReturn(List.of(STATION));

        mockMvc.perform(get("/api/v1/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(STATION_ID.toString()))
                .andExpect(jsonPath("$[0].stationNumber").value("1001"));
    }
}
