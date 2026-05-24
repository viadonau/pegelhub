package at.pegelhub.security;

import java.util.Set;

public record PegelHubActor(
        String subject,
        String clientId,
        Set<PegelHubAuthority> authorities
) {
    public PegelHubActor {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("subject must not be blank");
        }
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId must not be blank");
        }
        authorities = Set.copyOf(authorities);
    }

    public boolean hasAuthority(PegelHubAuthority authority) {
        return authorities.contains(authority);
    }
}
