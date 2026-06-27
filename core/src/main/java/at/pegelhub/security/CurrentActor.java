package at.pegelhub.security;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CurrentActor {

    static final String ACTOR_TYPE_CLAIM = "pegelhub_actor_type";

    public PegelHubActor get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new AuthenticationCredentialsNotFoundException("No JWT authentication is available");
        }
        return from(jwtAuthentication.getToken(), jwtAuthentication.getAuthorities());
    }

    PegelHubActor from(Jwt jwt, Collection<? extends GrantedAuthority> authorities) {
        String subject = clean(jwt.getSubject());
        String clientId = clientId(jwt);
        PegelHubActorType type = actorType(jwt);
        validateIdentity(type, subject, clientId);
        return new PegelHubActor(
                type,
                subject,
                clientId,
                pegelHubAuthorities(authorities));
    }

    private PegelHubActorType actorType(Jwt jwt) {
        String claim = clean(jwt.getClaimAsString(ACTOR_TYPE_CLAIM));
        if (claim == null) {
            throw new AuthenticationCredentialsNotFoundException("JWT has no " + ACTOR_TYPE_CLAIM + " claim");
        }
        try {
            return PegelHubActorType.valueOf(claim);
        } catch (IllegalArgumentException exception) {
            throw new AuthenticationCredentialsNotFoundException("JWT has an unsupported " + ACTOR_TYPE_CLAIM + " claim");
        }
    }

    private static void validateIdentity(PegelHubActorType type, String subject, String clientId) {
        if (type == PegelHubActorType.USER && subject == null) {
            throw new AuthenticationCredentialsNotFoundException("User JWT has no subject claim");
        }
        if (type == PegelHubActorType.CLIENT && clientId == null) {
            throw new AuthenticationCredentialsNotFoundException("Client JWT has no client id");
        }
    }

    private String clientId(Jwt jwt) {
        String authorizedParty = clean(jwt.getClaimAsString("azp"));
        if (authorizedParty != null) {
            return authorizedParty;
        }

        return clean(jwt.getClaimAsString("client_id"));
    }

    private Set<PegelHubAuthority> pegelHubAuthorities(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .map(PegelHubAuthority::from)
                .flatMap(Optional::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
