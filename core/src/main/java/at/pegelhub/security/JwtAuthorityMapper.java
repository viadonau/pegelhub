package at.pegelhub.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

@Component
public class JwtAuthorityMapper {

    public Converter<Jwt, AbstractAuthenticationToken> authenticationConverter() {
        return jwt -> new JwtAuthenticationToken(jwt, authorities(jwt), principalName(jwt));
    }

    Collection<GrantedAuthority> authorities(Jwt jwt) {
        return apiClientRoles(jwt).stream()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    private Collection<String> apiClientRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess == null) {
            return Set.of();
        }

        Object apiAccess = resourceAccess.get(PegelHubSecurityProperties.API_AUDIENCE);
        if (!(apiAccess instanceof Map<?, ?> accessByClient)) {
            return Set.of();
        }

        Object roles = accessByClient.get("roles");
        if (!(roles instanceof Collection<?> roleValues)) {
            return Set.of();
        }

        Collection<String> mappedRoles = new ArrayList<>();
        for (Object roleValue : roleValues) {
            if (roleValue instanceof String role && !role.isBlank()) {
                mappedRoles.add(role);
            }
        }
        return mappedRoles;
    }

    private String principalName(Jwt jwt) {
        String authorizedParty = jwt.getClaimAsString("azp");
        if (authorizedParty != null && !authorizedParty.isBlank()) {
            return authorizedParty;
        }
        return jwt.getSubject();
    }
}
