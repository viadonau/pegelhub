package at.pegelhub.telemetry.application;

import at.pegelhub.connector.domain.ConnectorStatus;
import at.pegelhub.security.CurrentActor;
import at.pegelhub.security.PegelHubActor;
import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.taker.domain.Taker;
import at.pegelhub.taker.persistence.TakerRepository;
import at.pegelhub.telemetry.domain.Telemetry;
import at.pegelhub.telemetry.persistence.TelemetryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static at.pegelhub.testsupport.ExampleData.TAKER;
import static at.pegelhub.testsupport.ExampleData.TELEMETRY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

final class TelemetryServiceImplTest {

    private TelemetryServiceImpl telemetryService;
    private static final TelemetryRepository REPOSITORY = mock(TelemetryRepository.class);
    private static final TakerRepository TAKER_REPOSITORY = mock(TakerRepository.class);
    private static final CurrentActor CURRENT_ACTOR = mock(CurrentActor.class);
    private static final PegelHubActor ACTOR = new PegelHubActor(
            "subject",
            "local-taker-example",
            Set.of());

    @BeforeEach
    public void prepare() {
        telemetryService = new TelemetryServiceImpl(REPOSITORY, TAKER_REPOSITORY, CURRENT_ACTOR);
        reset(REPOSITORY, TAKER_REPOSITORY, CURRENT_ACTOR);
        when(CURRENT_ACTOR.get()).thenReturn(ACTOR);
    }

    @Test
    public void constructorWithNullArgsThrowsNPE() {
        assertThrows(NullPointerException.class, () -> new TelemetryServiceImpl(null, TAKER_REPOSITORY, CURRENT_ACTOR));
        assertThrows(NullPointerException.class, () -> new TelemetryServiceImpl(REPOSITORY, null, CURRENT_ACTOR));
        assertThrows(NullPointerException.class, () -> new TelemetryServiceImpl(REPOSITORY, TAKER_REPOSITORY, null));
    }

    @Test
    public void saveTelemetry() {
        Telemetry savedTelemetry = new Telemetry(
                TAKER.getId().toString(),
                TELEMETRY.stationIPAddressIntern(),
                TELEMETRY.stationIPAddressExtern(),
                TELEMETRY.timestamp(),
                TELEMETRY.cycleTime(),
                TELEMETRY.temperatureWater(),
                TELEMETRY.temperatureAir(),
                TELEMETRY.performanceVoltageBattery(),
                TELEMETRY.performanceVoltageSupply(),
                TELEMETRY.performanceElectricityBattery(),
                TELEMETRY.performanceElectricitySupply(),
                TELEMETRY.fieldStrengthTransmission());
        when(TAKER_REPOSITORY.findByConnectorKeycloakClientId(ACTOR.clientId()))
                .thenReturn(java.util.Optional.of(TAKER));
        when(REPOSITORY.saveTelemetry(any())).thenReturn(savedTelemetry);

        Telemetry result = telemetryService.saveTelemetry(TELEMETRY);
        assertEquals(savedTelemetry, result);
        verify(TAKER_REPOSITORY).findByConnectorKeycloakClientId(ACTOR.clientId());
        verify(REPOSITORY).saveTelemetry(savedTelemetry);
    }

    @Test
    public void saveTelemetryThrowsIfConnectorIsNotRegistered() {
        when(TAKER_REPOSITORY.findByConnectorKeycloakClientId(ACTOR.clientId()))
                .thenReturn(java.util.Optional.empty());

        assertThrows(NotFoundException.class, () -> telemetryService.saveTelemetry(TELEMETRY));
        verify(REPOSITORY, never()).saveTelemetry(any());
    }

    @Test
    public void saveTelemetryThrowsIfConnectorIsInactive() {
        Taker inactiveTaker = new Taker(
                TAKER.getId(),
                TAKER.getStationNumber(),
                TAKER.getStationId(),
                TAKER.getTakerServiceManufacturer(),
                TAKER.getConnector().withExternalAuth("local-taker-example", ConnectorStatus.SUSPENDED),
                TAKER.getRefreshRate());
        when(TAKER_REPOSITORY.findByConnectorKeycloakClientId(ACTOR.clientId()))
                .thenReturn(java.util.Optional.of(inactiveTaker));

        assertThrows(AccessDeniedException.class, () -> telemetryService.saveTelemetry(TELEMETRY));
        verify(REPOSITORY, never()).saveTelemetry(any());
    }

    @Test
    public void getByRange() {
        when(REPOSITORY.getByRange(any())).thenReturn(List.of(TELEMETRY));

        List<Telemetry> result = telemetryService.getByRange("72d");
        assertEquals(1,result.size());
        assertEquals(TELEMETRY, result.getFirst());
        verify(REPOSITORY, times(1)).getByRange(any());
    }

    @Test
    public void getLastData() {
        when(REPOSITORY.getLastData(any())).thenReturn(TELEMETRY);

        Object result = telemetryService.getLastData(UUID.randomUUID());
        assertEquals(TELEMETRY, result);
        verify(REPOSITORY, times(1)).getLastData(any());
    }
}
