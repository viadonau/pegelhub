package at.pegelhub.telemetry.persistence;

import at.pegelhub.telemetry.domain.Telemetry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for all {@code Telemetry}s.
 */
public interface TelemetryRepository {

    /**
     * Saves a telemetry to the repository.
     *
     * @param telemetry to save.
     * @return the saved telemetry.
     */
    Telemetry saveTelemetry(Telemetry telemetry);

    /** Returns telemetry in the relative {@code range}. */
    List<Telemetry> getByRange(String range);

    /** Returns the latest telemetry for a connector identifier, if present. */
    Optional<Telemetry> getLastData(UUID connectorId);
}
