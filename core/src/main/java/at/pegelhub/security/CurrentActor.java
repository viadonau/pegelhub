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

    public PegelHubActor get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new AuthenticationCredentialsNotFoundException("No JWT authentication is available");
        }
        return from(jwtAuthentication.getToken(), jwtAuthentication.getAuthorities());
    }

    PegelHubActor from(Jwt jwt, Collection<? extends GrantedAuthority> authorities) {
        return new PegelHubActor(
                jwt.getSubject(),
                clientId(jwt),
                pegelHubAuthorities(authorities));
    }

    private String clientId(Jwt jwt) {
        String authorizedParty = jwt.getClaimAsString("azp");
        if (authorizedParty != null && !authorizedParty.isBlank()) {
            return authorizedParty;
        }

        String clientId = jwt.getClaimAsString("client_id");
        if (clientId != null && !clientId.isBlank()) {
            return clientId;
        }

        return jwt.getSubject();
    }

    private Set<PegelHubAuthority> pegelHubAuthorities(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .map(PegelHubAuthority::from)
                .flatMap(Optional::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }
}
