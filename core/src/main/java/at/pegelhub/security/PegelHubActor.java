package at.pegelhub.security;

import java.util.Set;

public record PegelHubActor(
        PegelHubActorType type,
        String subject,
        String clientId,
        Set<PegelHubAuthority> authorities
) {
    public PegelHubActor {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (type == PegelHubActorType.USER && (subject == null || subject.isBlank())) {
            throw new IllegalArgumentException("subject must not be blank for user actors");
        }
        if (type == PegelHubActorType.CLIENT && (clientId == null || clientId.isBlank())) {
            throw new IllegalArgumentException("clientId must not be blank");
        }
        authorities = Set.copyOf(authorities);
    }

    public boolean hasAuthority(PegelHubAuthority authority) {
        return authorities.contains(authority);
    }
}
