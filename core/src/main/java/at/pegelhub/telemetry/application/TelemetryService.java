package at.pegelhub.telemetry.application;

import at.pegelhub.telemetry.domain.Telemetry;

import java.util.List;
import java.util.UUID;

/**
 * Service for all {@code Telemetry}s.
 */
public interface TelemetryService {

    /** Saves telemetry for the authenticated connector. */
    Telemetry saveTelemetry(WriteTelemetryCommand command);

    /** Returns all connector telemetry in the relative {@code range}. */
    List<Telemetry> getByRange(String range);

    /** Returns the latest telemetry for a connector identifier. */
    Telemetry getLastData(UUID connectorId);
}
