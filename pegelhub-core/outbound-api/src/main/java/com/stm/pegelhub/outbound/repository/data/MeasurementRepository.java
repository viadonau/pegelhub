package com.stm.pegelhub.outbound.repository.data;


import com.stm.pegelhub.common.model.data.Measurement;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

/**
 * Repository for all {@code Measurement}s.
 */
public interface MeasurementRepository {

    /**
     * Saves multiple to the repository.
     *
     * @param measurements to save.
     * @return the saved measurement.
     */
    void storeMeasurements(List<Measurement> measurements);

    /**
     * Queries a measurement from the repository.
     *
     * @param range in which the returned values reside.
     * @return the measurements in that range.
     */
    List<Measurement> getByRange(String range);

    /**
     * Queries measurements from the repository.
     *
     * @param id of the measurement.
     * @param range in which the returned values reside.
     * @return the last measurement with that uuid.
     */
    List<Measurement> getByIDAndRange(UUID id, String range);

    /**
     * Queries the last measurement from the repository.
     *
     * @param uuid of the measurement.
     * @return the last measurement with that uuid.
     */
    Measurement getLastData(UUID uuid);

    /**
     * Calculates the average of all fields for a measurement over a given time range.
     *
     * @param id    of the measurement (supplier).
     * @param range in which to calculate the average.
     * @return a single measurement object with the averaged fields.
     */
    Measurement getAverageByIdAndRange(UUID id, String range);

    Timestamp getSystemTime();
}

