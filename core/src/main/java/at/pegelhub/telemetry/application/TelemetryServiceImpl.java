package at.pegelhub.telemetry.application;

import at.pegelhub.connector.domain.ConnectorStatus;
import at.pegelhub.security.CurrentActor;
import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.taker.domain.Taker;
import at.pegelhub.taker.persistence.TakerRepository;
import at.pegelhub.telemetry.domain.Telemetry;
import at.pegelhub.telemetry.persistence.TelemetryRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * Default implementation for {@code TelemetryService}.
 * Calls the repository-methods for storing and fetching the
 * telemtry data from the time series database
 * (Currently InfluxDB, name of bucket: telemetry=.
 */
@Service
public final class TelemetryServiceImpl implements TelemetryService {

    private final TelemetryRepository telemetryRepository;
    private final TakerRepository takerRepository;
    private final CurrentActor currentActor;

    public TelemetryServiceImpl(
            TelemetryRepository telemetryRepository,
            TakerRepository takerRepository,
            CurrentActor currentActor) {
        this.telemetryRepository = requireNonNull(telemetryRepository);
        this.takerRepository = requireNonNull(takerRepository);
        this.currentActor = requireNonNull(currentActor);
    }

    /**
     * calls the {@link TelemetryRepository#saveTelemetry(Telemetry)} method.
     * @param telemetry telemetry-data to save.
     * @return the save telemetry-data.
     */
    @Override
    public Telemetry saveTelemetry(Telemetry telemetry) {
        Taker taker = takerRepository.findByConnectorKeycloakClientId(currentActor.get().clientId())
                .orElseThrow(() -> new NotFoundException("Connector not registered"));
        if (taker.getConnector() == null || taker.getConnector().getStatus() != ConnectorStatus.ACTIVE) {
            throw new AccessDeniedException("Connector is not active");
        }
        Telemetry telemetryForTaker = new Telemetry(
                taker.getId().toString(),
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
        return telemetryRepository.saveTelemetry(telemetryForTaker);
    }

    /**
     * calls the {@link TelemetryRepository#getByRange(String)}  method.
     * @param range The {@link String} from which all entries in the speciefed range shall be returned.
     * @return a {@link List<Telemetry>} from the specified time range.
     */
    @Override
    public List<Telemetry> getByRange(String range) {
        return telemetryRepository.getByRange(range);
    }

    /**
     * calls the {@link TelemetryRepository#getLastData(UUID)}  method.
     * @param uuid The {@link UUID} from which the last entry shall be returned.
     * @return the last entry from the given {@link UUID}.
     */
    @Override
    public Telemetry getLastData(UUID uuid) {
        return telemetryRepository.getLastData(uuid);
    }
}
