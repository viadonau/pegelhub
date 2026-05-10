package at.pegelhub.supplier.api;

import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.supplier.application.StationManufacturerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static at.pegelhub.testsupport.ExampleData.STATION_MANUFACTURER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HttpStationManufacturerController.class)
class HttpStationManufacturerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StationManufacturerService stationManufacturerService;

    @Test
    void saveStationManufacturerReturnsDtoJson() throws Exception {
        when(stationManufacturerService.createStationManufacturer(any())).thenReturn(STATION_MANUFACTURER);

        mockMvc.perform(post("/api/v1/stationManufacturer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stationManufacturerName": "name",
                                  "stationManufacturerType": "type",
                                  "stationManufacturerFirmwareVersion": "1.0.0",
                                  "stationRemark": "remarks"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(STATION_MANUFACTURER.getId().toString()))
                .andExpect(jsonPath("$.stationManufacturerName").value(STATION_MANUFACTURER.getStationManufacturerName()));
    }

    @Test
    void getStationManufacturerByIdReturnsDtoJson() throws Exception {
        when(stationManufacturerService.getStationManufacturerById(STATION_MANUFACTURER.getId()))
                .thenReturn(STATION_MANUFACTURER);

        mockMvc.perform(get("/api/v1/stationManufacturer/{uuid}", STATION_MANUFACTURER.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(STATION_MANUFACTURER.getId().toString()));
    }

    @Test
    void getAllStationManufacturersReturnsArray() throws Exception {
        when(stationManufacturerService.getAllStationManufacturers()).thenReturn(List.of(STATION_MANUFACTURER));

        mockMvc.perform(get("/api/v1/stationManufacturer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(STATION_MANUFACTURER.getId().toString()));
    }

    @Test
    void deleteStationManufacturerDelegatesToService() throws Exception {
        mockMvc.perform(delete("/api/v1/stationManufacturer/{uuid}", STATION_MANUFACTURER.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(stationManufacturerService).deleteStationManufacturer(STATION_MANUFACTURER.getId());
    }

    @Test
    void getStationManufacturerByIdMapsNotFoundTo404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new NotFoundException("station manufacturer missing"))
                .when(stationManufacturerService)
                .getStationManufacturerById(id);

        mockMvc.perform(get("/api/v1/stationManufacturer/{uuid}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().string("station manufacturer missing"));
    }
}
