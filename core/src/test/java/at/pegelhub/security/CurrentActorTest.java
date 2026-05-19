package at.pegelhub.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentActorTest {

    private final CurrentActor currentActor = new CurrentActor();

    @Test
    void convertsJwtAuthenticationToActor() {
        Jwt jwt = jwt(Map.of("azp", "local-connector-example"));
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, List.of(
                new SimpleGrantedAuthority(PegelHubAuthority.MEASUREMENT_WRITE.value()),
                new SimpleGrantedAuthority(PegelHubAuthority.TELEMETRY_WRITE.value()),
                new SimpleGrantedAuthority("unknown:role")));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            PegelHubActor actor = currentActor.get();

            assertThat(actor.subject()).isEqualTo("service-account-subject");
            assertThat(actor.clientId()).isEqualTo("local-connector-example");
            assertThat(actor.authorities())
                    .containsExactlyInAnyOrder(PegelHubAuthority.MEASUREMENT_WRITE, PegelHubAuthority.TELEMETRY_WRITE);
            assertThat(actor.hasAuthority(PegelHubAuthority.MEASUREMENT_WRITE)).isTrue();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void usesClientIdClaimWhenAuthorizedPartyIsAbsent() {
        PegelHubActor actor = currentActor.from(
                jwt(Map.of("client_id", "connector-from-client-id")),
                List.of(new SimpleGrantedAuthority(PegelHubAuthority.METADATA_READ.value())));

        assertThat(actor.clientId()).isEqualTo("connector-from-client-id");
        assertThat(actor.authorities()).containsExactly(PegelHubAuthority.METADATA_READ);
    }

    @Test
    void requiresJwtAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("user", "password"));
        try {
            assertThatThrownBy(currentActor::get)
                    .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
                    .hasMessageContaining("No JWT authentication");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private static Jwt jwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("service-account-subject")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .claims(values -> values.putAll(claims))
                .build();
    }
}
