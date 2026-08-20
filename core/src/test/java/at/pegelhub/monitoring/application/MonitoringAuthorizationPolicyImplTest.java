package at.pegelhub.monitoring.application;

import at.pegelhub.security.CurrentActor;
import at.pegelhub.security.PegelHubActor;
import at.pegelhub.security.PegelHubActorType;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Set;

import static at.pegelhub.security.PegelHubAuthority.SYSTEM_ADMIN;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class MonitoringAuthorizationPolicyImplTest {

    @Test
    void rejectsConnectorsEvenWithSystemAdminAuthority() {
        CurrentActor currentActor = mock(CurrentActor.class);
        when(currentActor.get()).thenReturn(new PegelHubActor(
                PegelHubActorType.CLIENT, null, "connector", Set.of(SYSTEM_ADMIN)));

        assertThrows(AccessDeniedException.class,
                () -> new MonitoringAuthorizationPolicyImpl(currentActor).requireRead());
    }
}
