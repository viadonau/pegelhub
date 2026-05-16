package at.pegelhub.taker.api;

import at.pegelhub.auth.application.AuthTokenIdHolder;
import at.pegelhub.auth.application.AuthorizationService;
import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.shared.error.UnauthorizedException;
import at.pegelhub.taker.application.TakerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static at.pegelhub.testsupport.ExampleData.TAKER;
import static at.pegelhub.testsupport.ExampleDtos.CREATE_TAKER_DTO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
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
    private AuthorizationService authorizationService;

    @MockitoBean
    private TakerService takerService;

    @AfterEach
    void clearAuthHolder() {
        AuthTokenIdHolder.clear();
    }

    @Test
    void saveTakerUsesAuthContextAndClearsHolderAfterSuccess() throws Exception {
        UUID tokenId = UUID.randomUUID();
        when(authorizationService.authorize("valid")).thenReturn(tokenId);
        doAnswer(invocation -> {
            assertEquals(tokenId, AuthTokenIdHolder.get());
            return TAKER;
        }).when(takerService).saveTaker(any());

        mockMvc.perform(post("/api/v1/taker")
                        .param("apiKey", "valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(CREATE_TAKER_DTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TAKER.getId().toString()));

        assertNull(AuthTokenIdHolder.get());
    }

    @Test
    void saveTakerClearsHolderAfterException() throws Exception {
        UUID tokenId = UUID.randomUUID();
        when(authorizationService.authorize("valid")).thenReturn(tokenId);
        doAnswer(invocation -> {
            assertEquals(tokenId, AuthTokenIdHolder.get());
            throw new RuntimeException("save failed");
        }).when(takerService).saveTaker(any());

        mockMvc.perform(post("/api/v1/taker")
                        .param("apiKey", "valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(CREATE_TAKER_DTO)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("save failed"));

        assertNull(AuthTokenIdHolder.get());
    }

    @Test
    void getTakerByIdReturnsJson() throws Exception {
        when(takerService.getTakerById(TAKER.getId())).thenReturn(TAKER);

        mockMvc.perform(get("/api/v1/taker/{uuid}", TAKER.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TAKER.getId().toString()))
                .andExpect(jsonPath("$.stationNumber").value(TAKER.getStationNumber()));
    }

    @Test
    void getAllTakersReturnsArray() throws Exception {
        when(takerService.getAllTakers()).thenReturn(List.of(TAKER));

        mockMvc.perform(get("/api/v1/taker"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(TAKER.getId().toString()));
    }

    @Test
    void deleteTakerDelegatesToService() throws Exception {
        mockMvc.perform(delete("/api/v1/taker/{uuid}", TAKER.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(takerService).deleteTaker(TAKER.getId());
    }

    @Test
    void createTakerRejectsUnauthorizedRequest() throws Exception {
        doThrow(new UnauthorizedException("unauthorized")).when(authorizationService).authorize(any());

        mockMvc.perform(post("/api/v1/taker")
                        .param("apiKey", "invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(CREATE_TAKER_DTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Unauthorized"));
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
