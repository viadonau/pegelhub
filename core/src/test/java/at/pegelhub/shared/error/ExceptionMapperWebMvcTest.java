package at.pegelhub.shared.error;

import at.pegelhub.telemetry.api.HttpTelemetryController;
import at.pegelhub.telemetry.application.TelemetryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static at.pegelhub.testsupport.ExampleData.ID;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HttpTelemetryController.class)
class ExceptionMapperWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TelemetryService telemetryService;

    @Test
    void mapsUnauthorizedExceptionTo401() throws Exception {
        doThrow(new UnauthorizedException("unauthorized")).when(telemetryService).getByRange("72h");

        mockMvc.perform(get("/api/v1/telemetry/{range}", "72h"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("unauthorized"))
                .andExpect(jsonPath("$.instance").value("/api/v1/telemetry/72h"));
    }

    @Test
    void mapsNotFoundExceptionTo404() throws Exception {
        doThrow(new NotFoundException("missing")).when(telemetryService).getLastData(ID);

        mockMvc.perform(get("/api/v1/telemetry/last/{uuid}", ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("missing"));
    }

    @Test
    void mapsAccessDeniedExceptionTo403() throws Exception {
        doThrow(new AccessDeniedException("blocked")).when(telemetryService).getByRange("blocked");

        mockMvc.perform(get("/api/v1/telemetry/{range}", "blocked"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("Access is denied."));
    }

    @Test
    void mapsIllegalArgumentExceptionTo400() throws Exception {
        doThrow(new IllegalArgumentException("bad range")).when(telemetryService).getByRange("bad");

        mockMvc.perform(get("/api/v1/telemetry/{range}", "bad"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("bad range"));
    }

    @Test
    void mapsRuntimeExceptionTo500() throws Exception {
        doThrow(new RuntimeException("boom")).when(telemetryService).getByRange("72h");

        mockMvc.perform(get("/api/v1/telemetry/{range}", "72h"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."));
    }

    @Test
    void mapsInvalidPathVariableTo400() throws Exception {
        mockMvc.perform(get("/api/v1/telemetry/last/{uuid}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_PARAMETER"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[0].field").value("uuid"));
    }
}
