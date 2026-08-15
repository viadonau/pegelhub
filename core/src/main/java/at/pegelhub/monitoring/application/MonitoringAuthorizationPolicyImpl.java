package at.pegelhub.monitoring.application;

import at.pegelhub.security.CurrentActor;
import at.pegelhub.security.PegelHubActor;
import at.pegelhub.security.PegelHubActorType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import static at.pegelhub.security.PegelHubAuthority.MEASUREMENT_READ;
import static at.pegelhub.security.PegelHubAuthority.METADATA_READ;
import static at.pegelhub.security.PegelHubAuthority.SYSTEM_ADMIN;
import static java.util.Objects.requireNonNull;

@Service
final class MonitoringAuthorizationPolicyImpl implements MonitoringAuthorizationPolicy {

    private final CurrentActor currentActor;

    MonitoringAuthorizationPolicyImpl(CurrentActor currentActor) {
        this.currentActor = requireNonNull(currentActor);
    }

    @Override
    public void requireRead() {
        PegelHubActor actor = currentActor.get();
        if (actor.type() != PegelHubActorType.USER) {
            throw new AccessDeniedException("Connector actors are not allowed to read monitoring views");
        }
        if (!actor.hasAuthority(SYSTEM_ADMIN)
                && (!actor.hasAuthority(METADATA_READ) || !actor.hasAuthority(MEASUREMENT_READ))) {
            throw new AccessDeniedException("Actor is not allowed to read monitoring views");
        }
    }
}
