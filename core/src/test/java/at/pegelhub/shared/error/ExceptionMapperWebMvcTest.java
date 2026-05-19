package at.pegelhub.shared.error;

import at.pegelhub.telemetry.api.HttpTelemetryController;
import at.pegelhub.telemetry.application.TelemetryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static at.pegelhub.testsupport.ExampleData.ID;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
                .andExpect(content().string("Unauthorized"));
    }

    @Test
    void mapsNotFoundExceptionTo404() throws Exception {
        doThrow(new NotFoundException("missing")).when(telemetryService).getLastData(ID);

        mockMvc.perform(get("/api/v1/telemetry/last/{uuid}", ID))
                .andExpect(status().isNotFound())
                .andExpect(content().string("missing"));
    }

    @Test
    void mapsAccessDeniedExceptionTo403() throws Exception {
        doThrow(new AccessDeniedException("blocked")).when(telemetryService).getByRange("blocked");

        mockMvc.perform(get("/api/v1/telemetry/{range}", "blocked"))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Forbidden"));
    }

    @Test
    void mapsIllegalArgumentExceptionTo400() throws Exception {
        doThrow(new IllegalArgumentException("bad range")).when(telemetryService).getByRange("bad");

        mockMvc.perform(get("/api/v1/telemetry/{range}", "bad"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("bad range"));
    }

    @Test
    void mapsRuntimeExceptionTo500() throws Exception {
        doThrow(new RuntimeException("boom")).when(telemetryService).getByRange("72h");

        mockMvc.perform(get("/api/v1/telemetry/{range}", "72h"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("boom"));
    }
}
