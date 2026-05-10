package at.pegelhub.lib;

import at.pegelhub.lib.model.Measurement;

import java.net.URI;
import java.sql.Timestamp;
import java.util.*;

/**
 * API spec for measurement communication.
 */
public interface MeasurementAPI {
    /**
     * Get all telemetry data for the specified {@param timespan}.
     * The {@param timespan} needs a specific format. It begins with a number and ends with a time specification of that number.
     * Valid time specifications are:
     * * m - Minutes
     * * h - Hours
     * * d - Days
     * Examples:
     * 72h - 72 hours in the past
     * 720d - 720 days in the past
     * @param timespan the timespan from now to the past, which data is requested
     * @return Collection of {@code Measurement} containing found measurements in {@param timespan}.
     */
    Collection<Measurement> getMeasurements(String timespan);

    /**
     * @param uuid the identifier of a {@code Measurement} entry
     * @return {@code Optional} with {@code Measurement} or empty {@code Optional} if none match the {@param uuid}.
     */
    Optional<Measurement> getMeasurementByUUID(UUID uuid);

    /**
     * Get all measurement data for the specified {@param timespan}.
     * The {@param timespan} needs a specific format. It begins with a number and ends with a time specification of that number.
     * Valid time specifications are:
     * * m - Minutes
     * * h - Hours
     * * d - Days
     * Examples:
     * 72h - 72 hours in the past
     * 720d - 720 days in the past
     * @param stationNumber stationnumber of the supplier
     * @param timespan the timespan from now to the past, which data is requested
     * @return Collection of {@code Measurement} containing found measurements in {@param timespan}.
     */
    Collection<Measurement> getMeasurementsOfStation(String stationNumber, String timespan);

    /**
     * Get the latest measurement for configured takers station number.
     * @return the latest {@code Measurement} for the supplier
     */
    Optional<Measurement> getLatestMeasurementOfStation();

    HashSet<Long> getMeasurementsIDsOfStation(String stationNumber, String timespan);

    /**
     * Sends a list of ({@code Measurement}) to the core instance.
     * Throws {@code RuntimeException} when any errors occur.
     * @param meass the {@code Measurement}s to send
     */
    void sendMeasurements(List<Measurement> meass);

    Timestamp getSystemTime();

    Optional<Measurement> getTimestampOfLastMeasurementByUUID(UUID uuid);
}
