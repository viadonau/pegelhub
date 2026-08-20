package at.pegelhub.telemetry.application;

import at.pegelhub.connector.domain.Connector;
import at.pegelhub.shared.metadata.MetadataStatus;
import at.pegelhub.connector.persistence.ConnectorRepository;
import at.pegelhub.security.CurrentActor;
import at.pegelhub.security.PegelHubActorType;
import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.telemetry.domain.Telemetry;
import at.pegelhub.telemetry.persistence.TelemetryRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static at.pegelhub.security.PegelHubAuthority.SYSTEM_ADMIN;
import static at.pegelhub.security.PegelHubAuthority.TELEMETRY_READ;

@Service
public class TelemetryServiceImpl implements TelemetryService {

    private final TelemetryRepository telemetryRepository;
    private final ConnectorRepository connectorRepository;
    private final CurrentActor currentActor;

    public TelemetryServiceImpl(
            TelemetryRepository telemetryRepository,
            ConnectorRepository connectorRepository,
            CurrentActor currentActor) {
        this.telemetryRepository = requireNonNull(telemetryRepository);
        this.connectorRepository = requireNonNull(connectorRepository);
        this.currentActor = requireNonNull(currentActor);
    }

    @Override
    public Telemetry saveTelemetry(WriteTelemetryCommand command) {
        var actor = currentActor.get();
        if (actor.type() != PegelHubActorType.CLIENT) {
            throw new AccessDeniedException("Only connector clients may send telemetry");
        }
        Connector connector = connectorRepository.findByKeycloakClientId(actor.clientId())
                .orElseThrow(() -> new NotFoundException("Connector not registered"));
        if (connector.status() != MetadataStatus.ACTIVE) {
            throw new AccessDeniedException("Connector is not active");
        }
        Telemetry telemetryForConnector = new Telemetry(
                connector.id().value().toString(),
                command.stationIPAddressIntern(),
                command.stationIPAddressExtern(),
                command.timestamp(),
                command.cycleTime(),
                command.temperatureWater(),
                command.temperatureAir(),
                command.performanceVoltageBattery(),
                command.performanceVoltageSupply(),
                command.performanceElectricityBattery(),
                command.performanceElectricitySupply(),
                command.fieldStrengthTransmission());
        return telemetryRepository.saveTelemetry(telemetryForConnector);
    }

    @Override
    public List<Telemetry> getByRange(String range) {
        requireReadAccess();
        return telemetryRepository.getByRange(range);
    }

    @Override
    public Telemetry getLastData(UUID uuid) {
        requireNonNull(uuid);
        requireReadAccess();
        return telemetryRepository.getLastData(uuid)
                .orElseThrow(() -> new NotFoundException("No telemetry found for: " + uuid));
    }

    private void requireReadAccess() {
        var actor = currentActor.get();
        if (actor.type() == PegelHubActorType.USER
                && (actor.hasAuthority(TELEMETRY_READ) || actor.hasAuthority(SYSTEM_ADMIN))) {
            return;
        }
        throw new AccessDeniedException("Actor is not allowed to read telemetry");
    }
}
