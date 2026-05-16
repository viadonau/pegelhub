package at.pegelhub.taker.api;

import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.taker.application.TakerServiceManufacturerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static at.pegelhub.testsupport.ExampleData.TAKER_SERVICE_MANUFACTURER;
import static java.util.Objects.requireNonNull;
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

@WebMvcTest(HttpTakerServiceManufacturerController.class)
class HttpTakerServiceManufacturerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TakerServiceManufacturerService takerServiceManufacturerService;

    @Test
    void saveTakerServiceManufacturerReturnsDtoJson() throws Exception {
        when(takerServiceManufacturerService.createTakerServiceManufacturer(any())).thenReturn(TAKER_SERVICE_MANUFACTURER);

        mockMvc.perform(post("/api/v1/takerServiceManufacturer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "takerManufacturerName": "name",
                                  "takerSystemName": "name",
                                  "stationManufacturerFirmwareVersion": "1.0.0",
                                  "requestRemark": "remarks"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requireNonNull(TAKER_SERVICE_MANUFACTURER.getId()).toString()))
                .andExpect(jsonPath("$.takerManufacturerName").value(TAKER_SERVICE_MANUFACTURER.getTakerManufacturerName()));
    }

    @Test
    void getTakerServiceManufacturerByIdReturnsDtoJson() throws Exception {
        when(takerServiceManufacturerService.getTakerServiceManufacturerById(TAKER_SERVICE_MANUFACTURER.getId()))
                .thenReturn(TAKER_SERVICE_MANUFACTURER);

        mockMvc.perform(get("/api/v1/takerServiceManufacturer/{uuid}", TAKER_SERVICE_MANUFACTURER.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requireNonNull(TAKER_SERVICE_MANUFACTURER.getId()).toString()));
    }

    @Test
    void getAllTakerServiceManufacturersReturnsArray() throws Exception {
        when(takerServiceManufacturerService.getAllTakerServiceManufacturers())
                .thenReturn(List.of(TAKER_SERVICE_MANUFACTURER));

        mockMvc.perform(get("/api/v1/takerServiceManufacturer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(requireNonNull(TAKER_SERVICE_MANUFACTURER.getId()).toString()));
    }

    @Test
    void deleteTakerServiceManufacturerDelegatesToService() throws Exception {
        mockMvc.perform(delete("/api/v1/takerServiceManufacturer/{uuid}", TAKER_SERVICE_MANUFACTURER.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(takerServiceManufacturerService).deleteTakerServiceManufacturer(TAKER_SERVICE_MANUFACTURER.getId());
    }

    @Test
    void getTakerServiceManufacturerByIdMapsNotFoundTo404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new NotFoundException("taker service manufacturer missing"))
                .when(takerServiceManufacturerService)
                .getTakerServiceManufacturerById(id);

        mockMvc.perform(get("/api/v1/takerServiceManufacturer/{uuid}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().string("taker service manufacturer missing"));
    }
}
