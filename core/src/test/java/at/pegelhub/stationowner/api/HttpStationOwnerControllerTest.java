package at.pegelhub.stationowner.api;

import at.pegelhub.stationowner.application.CreateStationOwnerCommand;
import at.pegelhub.stationowner.application.StationOwnerService;
import at.pegelhub.stationowner.domain.StationOwner;
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

@WebMvcTest(HttpStationOwnerController.class)
class HttpStationOwnerControllerTest {

    private static final UUID OWNER_ID = UUID.fromString("b2a69ffa-36e3-4bba-98f0-d490fb121dc0");
    private static final StationOwner OWNER = new StationOwner(
            new StationOwnerId(OWNER_ID),
            "Hydro Org",
            "HO",
            "notes");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StationOwnerService stationOwners;

    @Test
    void createsStationOwner() throws Exception {
        when(stationOwners.create(any())).thenReturn(OWNER);

        mockMvc.perform(post("/api/v1/station-owners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Hydro Org",
                                  "shortName": "HO",
                                  "notes": "notes"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(OWNER_ID.toString()))
                .andExpect(jsonPath("$.name").value("Hydro Org"))
                .andExpect(jsonPath("$.shortName").value("HO"))
                .andExpect(jsonPath("$.notes").value("notes"));

        verify(stationOwners).create(eq(new CreateStationOwnerCommand("Hydro Org", "HO", "notes")));
    }

    @Test
    void rejectsCreateWithoutName() throws Exception {
        mockMvc.perform(post("/api/v1/station-owners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shortName": "HO"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getsStationOwner() throws Exception {
        when(stationOwners.get(new StationOwnerId(OWNER_ID))).thenReturn(OWNER);

        mockMvc.perform(get("/api/v1/station-owners/{id}", OWNER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(OWNER_ID.toString()))
                .andExpect(jsonPath("$.name").value("Hydro Org"));
    }

    @Test
    void listsStationOwners() throws Exception {
        when(stationOwners.list()).thenReturn(List.of(OWNER));

        mockMvc.perform(get("/api/v1/station-owners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(OWNER_ID.toString()))
                .andExpect(jsonPath("$[0].name").value("Hydro Org"));
    }
}
