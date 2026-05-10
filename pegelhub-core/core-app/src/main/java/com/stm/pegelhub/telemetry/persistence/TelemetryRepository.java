package com.stm.pegelhub.telemetry.persistence;

import com.stm.pegelhub.telemetry.domain.Telemetry;

import java.util.List;
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

    /**
     * Queries a telemetry from the repository.
     *
     * @param range in which the returned values reside.
     * @return the telemetries in that range.
     */
    List<Telemetry> getByRange(String range);

    /**
     * Queries the last telemetry from the repository.
     *
     * @param uuid of the telemetry.
     * @return the last telemetry with that uuid.
     */
    Telemetry getLastData(UUID uuid);
}

