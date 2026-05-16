package at.pegelhub.shared.error;

import at.pegelhub.auth.api.HttpApiTokenController;
import at.pegelhub.auth.application.ApiTokenService;
import at.pegelhub.auth.application.AuthorizationService;
import at.pegelhub.telemetry.api.HttpTelemetryController;
import at.pegelhub.telemetry.application.TelemetryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static at.pegelhub.testsupport.ExampleData.ID;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({HttpTelemetryController.class, HttpApiTokenController.class})
class ExceptionMapperWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthorizationService authorizationService;

    @MockitoBean
    private TelemetryService telemetryService;

    @MockitoBean
    private ApiTokenService apiTokenService;

    @Test
    void mapsUnauthorizedExceptionTo401() throws Exception {
        doThrow(new UnauthorizedException("unauthorized")).when(authorizationService).authorize("bad");

        mockMvc.perform(get("/api/v1/telemetry/{range}", "72h")
                        .param("apiKey", "bad"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Unauthorized"));
    }

    @Test
    void mapsNotFoundExceptionTo404() throws Exception {
        when(authorizationService.authorize("good")).thenReturn(UUID.randomUUID());
        doThrow(new NotFoundException("missing")).when(telemetryService).getLastData(ID);

        mockMvc.perform(get("/api/v1/telemetry/last/{uuid}", ID)
                        .param("apiKey", "good"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("missing"));
    }

    @Test
    void mapsIllegalArgumentExceptionTo400() throws Exception {
        mockMvc.perform(put("/api/v1/token")
                        .param("apiKey", "any")
                        .param("uuid", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mapsRuntimeExceptionTo500() throws Exception {
        when(authorizationService.authorize("good")).thenReturn(UUID.randomUUID());
        doThrow(new RuntimeException("boom")).when(telemetryService).getByRange("72h");

        mockMvc.perform(get("/api/v1/telemetry/{range}", "72h")
                        .param("apiKey", "good"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("boom"));
    }
}
