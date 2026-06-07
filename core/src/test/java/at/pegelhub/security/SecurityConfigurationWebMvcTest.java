package at.pegelhub.security;

import at.pegelhub.connector.api.HttpAdminConnectorController;
import at.pegelhub.connector.application.ConnectorService;
import at.pegelhub.connector.domain.ConnectorStatus;
import at.pegelhub.measurement.api.HttpMeasurementController;
import at.pegelhub.measurement.application.MeasurementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static at.pegelhub.testsupport.ExampleData.CONNECTOR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({HttpMeasurementController.class, HttpAdminConnectorController.class})
@Import({SecurityConfiguration.class, JwtAuthorityMapper.class})
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@TestPropertySource(properties = {
        "pegelhub.security.issuer-uri=http://issuer.test/realms/pegelhub",
        "pegelhub.security.audience=pegelhub-core-api",
        "pegelhub.security.api-client-id=pegelhub-core-api"
})
class SecurityConfigurationWebMvcTest {

    private static final String ISSUER = "http://issuer.test/realms/pegelhub";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private MeasurementService measurementService;

    @MockitoBean
    private ConnectorService connectorService;

    @Test
    void protectedApiReturnsUnauthorizedWhenTokenIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/measurement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(measurementsJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedApiReturnsForbiddenWhenRoleIsMissing() throws Exception {
        when(jwtDecoder.decode("metadata-token")).thenReturn(jwt(
                "metadata-token",
                "local-operator",
                List.of(PegelHubAuthority.METADATA_READ.value())));

        mockMvc.perform(post("/api/v1/measurement")
                        .header("Authorization", "Bearer metadata-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(measurementsJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedApiReturnsUnauthorizedWhenAudienceIsInvalid() throws Exception {
        when(jwtDecoder.decode("wrong-audience")).thenThrow(validationException("Token audience must contain pegelhub-core-api"));

        mockMvc.perform(post("/api/v1/measurement")
                        .header("Authorization", "Bearer wrong-audience")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(measurementsJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedApiReturnsUnauthorizedWhenIssuerIsInvalid() throws Exception {
        when(jwtDecoder.decode("wrong-issuer")).thenThrow(validationException("Token issuer is invalid"));

        mockMvc.perform(post("/api/v1/measurement")
                        .header("Authorization", "Bearer wrong-issuer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(measurementsJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedApiAllowsValidJwtWithRequiredRole() throws Exception {
        when(jwtDecoder.decode("measurement-token"))
                .thenReturn(jwt(
                        "measurement-token",
                        "local-connector-example",
                        List.of(PegelHubAuthority.MEASUREMENT_WRITE.value())));

        mockMvc.perform(post("/api/v1/measurement")
                        .header("Authorization", "Bearer measurement-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(measurementsJson()))
                .andExpect(status().isOk());
    }

    @Test
    void publicSystemTimeDoesNotRequireToken() throws Exception {
        mockMvc.perform(get("/api/v1/measurement/systemTime"))
                .andExpect(status().isOk());
    }

    @Test
    void connectorTokenCannotRegisterConnectorIdentity() throws Exception {
        when(jwtDecoder.decode("connector-token"))
                .thenReturn(jwt(
                        "connector-token",
                        "local-connector-example",
                        List.of(PegelHubAuthority.MEASUREMENT_WRITE.value())));

        mockMvc.perform(post("/api/v1/admin/connectors")
                        .header("Authorization", "Bearer connector-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerConnectorJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void operatorTokenCanRegisterConnectorIdentity() throws Exception {
        when(jwtDecoder.decode("operator-token"))
                .thenReturn(jwt(
                        "operator-token",
                        "local-operator",
                        List.of(PegelHubAuthority.SYSTEM_ADMIN.value())));
        when(connectorService.registerConnector(anyString(), any(), any()))
                .thenReturn(CONNECTOR.withExternalAuth("local-connector-example", ConnectorStatus.ACTIVE));

        mockMvc.perform(post("/api/v1/admin/connectors")
                        .header("Authorization", "Bearer operator-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerConnectorJson()))
                .andExpect(status().isCreated());
    }

    private static Jwt jwt(String tokenValue, String authorizedParty, List<String> roles) {
        return Jwt.withTokenValue(tokenValue)
                .header("alg", "none")
                .issuer(ISSUER)
                .subject("subject")
                .audience(List.of("pegelhub-core-api"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .claim("azp", authorizedParty)
                .claim("resource_access", Map.of("pegelhub-core-api", Map.of("roles", roles)))
                .build();
    }

    private static JwtValidationException validationException(String description) {
        return new JwtValidationException(
                description,
                List.of(new OAuth2Error("invalid_token", description, null)));
    }

    private static String measurementsJson() {
        return """
                {
                  "measurements": [
                    {
                      "timeSeriesId": "8ce8c5b6-f093-4d46-b770-7239cdfa3d76",
                      "observedAt": "2026-04-25T10:15:30Z",
                      "value": 10.5
                    }
                  ]
                }
                """;
    }

    private static String registerConnectorJson() {
        return """
                {
                  "keycloakClientId": "local-connector-example",
                  "connector": {
                    "connectorNumber": "connectorNR",
                    "manufacturer": {
                      "organization": "org1"
                    },
                    "typeDescription": "description",
                    "softwareVersion": "1.0.0",
                    "worksFromDataVersion": "1.0.0",
                    "dataDefinition": "definition",
                    "softwareManufacturer": {
                      "organization": "org1"
                    },
                    "technicallyResponsible": {
                      "organization": "org1"
                    },
                    "operationCompany": {
                      "organization": "org1"
                    },
                    "notes": "notes"
                  }
                }
                """;
    }
}
