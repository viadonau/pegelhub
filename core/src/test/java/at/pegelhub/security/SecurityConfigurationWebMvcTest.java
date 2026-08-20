package at.pegelhub.security;

import at.pegelhub.timeseries.api.ObservedPropertyController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ObservedPropertyController.class)
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@Import({SecurityConfiguration.class, JwtAuthorityMapper.class})
@TestPropertySource(properties = "pegelhub.security.issuer-uri=http://issuer.test/realms/pegelhub")
class SecurityConfigurationWebMvcTest {
    private static final String ISSUER = "http://issuer.test/realms/pegelhub";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void metadataReadRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/observed-properties"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void connectorCannotUseUserMetadataRoute() throws Exception {
        when(jwtDecoder.decode("connector-token")).thenReturn(jwt("connector-token", "CLIENT"));

        mockMvc.perform(get("/api/v1/observed-properties")
                        .header("Authorization", "Bearer connector-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void operatorCanReadMetadata() throws Exception {
        when(jwtDecoder.decode("operator-token")).thenReturn(jwt("operator-token", "USER"));

        mockMvc.perform(get("/api/v1/observed-properties")
                        .header("Authorization", "Bearer operator-token"))
                .andExpect(status().isOk());
    }

    private static Jwt jwt(String token, String actorType) {
        return Jwt.withTokenValue(token)
                .header("alg", "none")
                .issuer(ISSUER)
                .subject("operator")
                .audience(List.of(PegelHubSecurityProperties.API_AUDIENCE))
                .issuedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .expiresAt(Instant.parse("2026-01-01T01:00:00Z"))
                .claim("azp", actorType.equals("CLIENT") ? "connector-client" : "operator-client")
                .claim(CurrentActor.ACTOR_TYPE_CLAIM, actorType)
                .claim("resource_access", Map.of(PegelHubSecurityProperties.API_AUDIENCE,
                        Map.of("roles", List.of(PegelHubAuthority.METADATA_READ.value()))))
                .build();
    }
}
