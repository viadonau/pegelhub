package at.pegelhub.security;

import at.pegelhub.notifications.api.NotificationController;
import at.pegelhub.notifications.application.CredentialCatalog;
import at.pegelhub.notifications.application.NotificationProperties;
import at.pegelhub.notifications.application.Notifications;
import at.pegelhub.quality.api.QualityController;
import at.pegelhub.quality.application.Quality;
import at.pegelhub.quality.application.QualityProperties;
import at.pegelhub.shared.api.ConfigurationYaml;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({QualityController.class, NotificationController.class})
@ImportAutoConfiguration({SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
@Import({SecurityConfiguration.class, JwtAuthorityMapper.class})
@TestPropertySource(properties = "pegelhub.security.issuer-uri=http://issuer.test/realms/pegelhub")
class OperationalSecurityTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder decoder;

    @MockitoBean
    Quality quality;

    @MockitoBean
    QualityProperties qualityProperties;

    @MockitoBean
    Notifications notifications;

    @MockitoBean
    NotificationProperties notificationProperties;

    @MockitoBean
    CredentialCatalog credentials;

    @MockitoBean
    ConfigurationYaml yaml;


    @ParameterizedTest
    @CsvSource({"USER,system:admin,/api/v1/quality/profiles,200", "USER,metadata:write,/api/v1/quality/profiles,403",
            "CLIENT,system:admin,/api/v1/quality/profiles,403", "USER,metadata:read;/measurement:read,/api/v1/quality/runs,200",
            "USER,metadata:read,/api/v1/quality/runs,403", "CLIENT,measurement:read;/metadata:read,/api/v1/quality/runs,403",
            "USER,system:admin,/api/v1/notifications/deliveries,200", "USER,metadata:read,/api/v1/notifications/deliveries,403"})
    void readRoutesEnforceActorAndRoles(String actor, String roles, String path, int expected) throws Exception {
        token(actor, roles);

        mvc.perform(get(path).header("Authorization", "Bearer test")).andExpect(status().is(expected));
    }

    @ParameterizedTest
    @CsvSource({"CLIENT,messaging:send,202", "CLIENT,system:admin,403", "USER,messaging:send,403", "USER,system:admin,202"})
    void submissionHasItsOwnNarrowServicePermission(String actor, String roles, int expected) throws Exception {
        token(actor, roles);

        mvc.perform(post("/api/v1/notifications")
                        .header("Authorization", "Bearer test")
                        .contentType("application/json")
                        .content("{\"destinations\":[],\"subject\":\"Test\",\"body\":\"Test\"}"))
                .andExpect(status().is(expected));
    }

    private void token(String actor, String roles) {
        when(decoder.decode("test")).thenReturn(Jwt.withTokenValue("test").header("alg", "none")
                .subject("test").claim(CurrentActor.ACTOR_TYPE_CLAIM, actor)
                .claim("resource_access", Map.of(PegelHubSecurityProperties.API_AUDIENCE,
                        Map.of("roles", List.of(roles.split(";/"))))).build());
    }
}
