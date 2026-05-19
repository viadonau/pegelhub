package at.pegelhub.taker.api;

import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.taker.application.TakerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static at.pegelhub.testsupport.ExampleData.TAKER;
import static java.util.Objects.requireNonNull;
import static at.pegelhub.testsupport.ExampleDtos.CREATE_TAKER_DTO;
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
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(HttpTakerController.class)
class HttpTakerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @MockitoBean
    private TakerService takerService;

    @Test
    void saveTakerReturnsTakerJson() throws Exception {
        when(takerService.saveTaker(any())).thenReturn(TAKER);

        mockMvc.perform(post("/api/v1/taker")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(CREATE_TAKER_DTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requireNonNull(TAKER.getId()).toString()));
    }

    @Test
    void saveTakerMapsServiceExceptionTo500() throws Exception {
        doThrow(new RuntimeException("save failed")).when(takerService).saveTaker(any());

        mockMvc.perform(post("/api/v1/taker")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(CREATE_TAKER_DTO)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("save failed"));
    }

    @Test
    void getTakerByIdReturnsJson() throws Exception {
        when(takerService.getTakerById(TAKER.getId())).thenReturn(TAKER);

        mockMvc.perform(get("/api/v1/taker/{uuid}", TAKER.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requireNonNull(TAKER.getId()).toString()))
                .andExpect(jsonPath("$.stationNumber").value(TAKER.getStationNumber()));
    }

    @Test
    void getAllTakersReturnsArray() throws Exception {
        when(takerService.getAllTakers()).thenReturn(List.of(TAKER));

        mockMvc.perform(get("/api/v1/taker"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(requireNonNull(TAKER.getId()).toString()));
    }

    @Test
    void deleteTakerDelegatesToService() throws Exception {
        mockMvc.perform(delete("/api/v1/taker/{uuid}", TAKER.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(takerService).deleteTaker(TAKER.getId());
    }

    @Test
    void getTakerByIdMapsNotFoundTo404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new NotFoundException("taker missing")).when(takerService).getTakerById(id);

        mockMvc.perform(get("/api/v1/taker/{uuid}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().string("taker missing"));
    }
}
