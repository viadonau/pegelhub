package at.pegelhub.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthorityMapperTest {

    private final JwtAuthorityMapper mapper = new JwtAuthorityMapper();

    @Test
    void mapsApiClientRolesAndScopesToAuthorities() {
        Jwt jwt = jwt(Map.of(
                "resource_access", Map.of(
                        "pegelhub-core-api", Map.of("roles", List.of(
                                PegelHubAuthority.MEASUREMENT_WRITE.value(),
                                PegelHubAuthority.TELEMETRY_WRITE.value()))),
                "scope", PegelHubAuthority.METADATA_READ.value(),
                "scp", List.of(PegelHubAuthority.SYSTEM_ADMIN.value())));

        assertThat(mapper.authorities(jwt))
                .extracting(Object::toString)
                .containsExactly(
                        PegelHubAuthority.MEASUREMENT_WRITE.value(),
                        PegelHubAuthority.TELEMETRY_WRITE.value(),
                        PegelHubAuthority.METADATA_READ.value(),
                        PegelHubAuthority.SYSTEM_ADMIN.value());
    }

    @Test
    void ignoresRolesForOtherClients() {
        Jwt jwt = jwt(Map.of(
                "resource_access", Map.of(
                        "other-api", Map.of("roles", List.of(PegelHubAuthority.MEASUREMENT_WRITE.value())))));

        assertThat(mapper.authorities(jwt)).isEmpty();
    }

    private static Jwt jwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuer("http://issuer.test/realms/pegelhub")
                .subject("subject")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .claims(values -> values.putAll(claims))
                .build();
    }
}
