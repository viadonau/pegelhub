package at.pegelhub.telemetry.application;

import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.domain.ConnectorStatus;
import at.pegelhub.connector.persistence.ConnectorRepository;
import at.pegelhub.security.CurrentActor;
import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.telemetry.domain.Telemetry;
import at.pegelhub.telemetry.persistence.TelemetryRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@Service
public final class TelemetryServiceImpl implements TelemetryService {

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
    public Telemetry saveTelemetry(Telemetry telemetry) {
        Connector connector = connectorRepository.findByKeycloakClientId(currentActor.get().clientId())
                .orElseThrow(() -> new NotFoundException("Connector not registered"));
        if (connector.getStatus() != ConnectorStatus.ACTIVE) {
            throw new AccessDeniedException("Connector is not active");
        }
        Telemetry telemetryForConnector = new Telemetry(
                connector.getId().toString(),
                telemetry.stationIPAddressIntern(),
                telemetry.stationIPAddressExtern(),
                telemetry.timestamp(),
                telemetry.cycleTime(),
                telemetry.temperatureWater(),
                telemetry.temperatureAir(),
                telemetry.performanceVoltageBattery(),
                telemetry.performanceVoltageSupply(),
                telemetry.performanceElectricityBattery(),
                telemetry.performanceElectricitySupply(),
                telemetry.fieldStrengthTransmission());
        return telemetryRepository.saveTelemetry(telemetryForConnector);
    }

    @Override
    public List<Telemetry> getByRange(String range) {
        return telemetryRepository.getByRange(range);
    }

    @Override
    public Telemetry getLastData(UUID uuid) {
        return telemetryRepository.getLastData(uuid);
    }
}
