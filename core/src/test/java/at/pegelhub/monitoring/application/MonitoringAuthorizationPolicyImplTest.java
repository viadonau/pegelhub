package at.pegelhub.monitoring.application;

import at.pegelhub.security.CurrentActor;
import at.pegelhub.security.PegelHubActor;
import at.pegelhub.security.PegelHubActorType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static at.pegelhub.security.PegelHubAuthority.MEASUREMENT_READ;
import static at.pegelhub.security.PegelHubAuthority.METADATA_READ;
import static at.pegelhub.security.PegelHubAuthority.SYSTEM_ADMIN;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class MonitoringAuthorizationPolicyImplTest {

    private final CurrentActor currentActor = mock(CurrentActor.class);
    private final MonitoringAuthorizationPolicyImpl policy = new MonitoringAuthorizationPolicyImpl(currentActor);

    @Test
    void permitsOperatorWithBothReadAuthorities() {
        when(currentActor.get()).thenReturn(new PegelHubActor(
                PegelHubActorType.USER,
                "operator",
                null,
                Set.of(METADATA_READ, MEASUREMENT_READ)));

        assertDoesNotThrow(policy::requireRead);
    }

    @Test
    void permitsUserSystemAdmin() {
        when(currentActor.get()).thenReturn(new PegelHubActor(
                PegelHubActorType.USER, "admin", null, Set.of(SYSTEM_ADMIN)));

        assertDoesNotThrow(policy::requireRead);
    }

    @Test
    void rejectsConnectorsEvenWithSystemAdminAuthority() {
        when(currentActor.get()).thenReturn(new PegelHubActor(
                PegelHubActorType.CLIENT, null, "connector", Set.of(SYSTEM_ADMIN)));

        assertThrows(org.springframework.security.access.AccessDeniedException.class, policy::requireRead);
    }

    @Test
    void rejectsUsersMissingEitherReadAuthority() {
        when(currentActor.get()).thenReturn(new PegelHubActor(
                PegelHubActorType.USER, "operator", null, Set.of(METADATA_READ)));

        assertThrows(org.springframework.security.access.AccessDeniedException.class, policy::requireRead);
    }
}
