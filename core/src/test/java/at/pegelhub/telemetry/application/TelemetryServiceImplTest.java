package at.pegelhub.telemetry.application;

import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.domain.ConnectorType;
import at.pegelhub.connector.persistence.ConnectorRepository;
import at.pegelhub.security.CurrentActor;
import at.pegelhub.security.PegelHubActor;
import at.pegelhub.security.PegelHubActorType;
import at.pegelhub.security.PegelHubAuthority;
import at.pegelhub.shared.metadata.MetadataStatus;
import at.pegelhub.telemetry.persistence.TelemetryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TelemetryServiceImplTest {
    private final TelemetryRepository telemetry = mock(TelemetryRepository.class);
    private final ConnectorRepository connectors = mock(ConnectorRepository.class);
    private final CurrentActor actor = mock(CurrentActor.class);
    private final TelemetryServiceImpl service = new TelemetryServiceImpl(telemetry, connectors, actor);

    @Test
    void connectorCannotReadTelemetry() {
        when(actor.get()).thenReturn(new PegelHubActor(
                PegelHubActorType.CLIENT, null, "connector-client", Set.of(PegelHubAuthority.TELEMETRY_READ)));

        assertThatThrownBy(() -> service.getByRange("24h"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void operatorWithTelemetryReadMayReadTelemetry() {
        when(actor.get()).thenReturn(new PegelHubActor(
                PegelHubActorType.USER, "operator", null, Set.of(PegelHubAuthority.TELEMETRY_READ)));
        when(telemetry.getByRange("24h")).thenReturn(List.of());

        service.getByRange("24h");

        verify(telemetry).getByRange("24h");
    }

    @Test
    void inactiveConnectorCannotSendTelemetry() {
        when(actor.get()).thenReturn(new PegelHubActor(
                PegelHubActorType.CLIENT, null, "connector-client", Set.of(PegelHubAuthority.TELEMETRY_WRITE)));
        Connector connector = Connector.create("Connector", ConnectorType.OTHER)
                .bind("connector-client", MetadataStatus.INACTIVE);
        when(connectors.findByKeycloakClientId("connector-client")).thenReturn(Optional.of(connector));

        assertThatThrownBy(() -> service.saveTelemetry(new WriteTelemetryCommand(
                "10.0.0.1", "10.0.0.2", java.time.Instant.parse("2026-01-01T00:00:00Z"),
                1, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0)))
                .isInstanceOf(AccessDeniedException.class);
    }
}
