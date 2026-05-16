package at.pegelhub.auth.api;

import at.pegelhub.auth.application.ApiTokenService;
import at.pegelhub.shared.error.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HttpApiTokenController.class)
class HttpApiTokenControllerTest {

    private static final String TOKEN = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApiTokenService apiTokenService;

    @Test
    void createTokenReturnsApiKeyJson() throws Exception {
        when(apiTokenService.createToken()).thenReturn(TOKEN);

        mockMvc.perform(post("/api/v1/token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiKey").value(TOKEN));
    }

    @Test
    void refreshTokenReturnsApiKeyJson() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(apiTokenService.refreshToken("valid", uuid)).thenReturn(TOKEN);

        mockMvc.perform(put("/api/v1/token")
                        .param("apiKey", "valid")
                        .param("uuid", uuid.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiKey").value(TOKEN));
    }

    @Test
    void invalidateTokenDelegatesToService() throws Exception {
        UUID uuid = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/token")
                        .param("apiKey", "valid")
                        .param("uuid", uuid.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(apiTokenService).invalidateToken("valid", uuid);
    }

    @Test
    void getTokensReturnsUuidArray() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(apiTokenService.getTokens()).thenReturn(List.of(id1, id2));

        mockMvc.perform(get("/api/v1/token/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(id1.toString()))
                .andExpect(jsonPath("$[1]").value(id2.toString()));
    }

    @Test
    void activateTokenDelegatesToService() throws Exception {
        UUID uuid = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/token/admin")
                        .param("uuid", uuid.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(apiTokenService).activateToken(uuid);
    }

    @Test
    void refreshTokenWithInvalidUuidReturnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/v1/token")
                        .param("apiKey", "valid")
                        .param("uuid", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidateTokenMapsNotFoundTo404() throws Exception {
        UUID uuid = UUID.randomUUID();
        doThrow(new NotFoundException("missing token"))
                .when(apiTokenService)
                .invalidateToken("valid", uuid);

        mockMvc.perform(delete("/api/v1/token")
                        .param("apiKey", "valid")
                        .param("uuid", uuid.toString()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("missing token"));
    }
}
