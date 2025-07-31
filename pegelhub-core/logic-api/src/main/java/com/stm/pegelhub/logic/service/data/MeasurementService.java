package com.stm.pegelhub.logic.service.data;

import com.stm.pegelhub.common.model.data.Measurement;
import com.stm.pegelhub.logic.domain.WriteMeasurements;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

/**
 * Service for all {@code Measurement}s.
 */
public interface MeasurementService {

    /**
     * Saves multiple measurements.
     *
     * @param measurements to save.
     */
    void writeMeasurements(WriteMeasurements measurements);

    /**
     * Queries a measurement.
     *
     * @param range in which the returned values reside.
     * @return the measurements in that range.
     */
    List<Measurement> getByRange(String range);

    /**
     * Queries all measurement in range for a given supplier.
     *
     * @param stationNumber of the supplier
     * @param range in which the returned values reside.
     * @return the measurements in that range.
     */
    List<Measurement> getBySupplierAndRange(String stationNumber, String range);

    /**
     * Queries the latest measurement for a given supplier.
     *
     * @param stationNumber of the supplier
     * @return the latest measurements.
     */
    Measurement getLatestBySupplier(String stationNumber);

    /**
     * Queries the last measurement.
     *
     * @param uuid of the measurement.
     * @return the last measurement with that uuid.
     */
    Measurement getLastData(UUID uuid);

    Timestamp getSystemTime();
}

