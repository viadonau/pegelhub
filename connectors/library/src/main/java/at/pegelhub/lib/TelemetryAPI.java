package at.pegelhub.lib;

import at.pegelhub.lib.model.Telemetry;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * API spec for telemetry communication.
 */
public interface TelemetryAPI {
    /**
     * Get all telemetry data for the specified timespan.
     * The timespan needs a specific format. It begins with a number and ends with a time specification of that number.
     * Valid time specifications are:
     * * m - Minutes
     * * h - Hours
     * * d - Days
     * Examples:
     * 72h - 72 hours in the past
     * 720d - 720 days in the past
     * @param timespan the timespan from now to the past, which data is requested
     * @return Collection of {@code Telemetry} containing found telemetries in {@param timespan}.
     */
    Collection<Telemetry> getTelemetry(String timespan);

    /**
     * @param uuid the identifier of a {@code Telemetry} entry
     * @return {@code Optional} with {@code Telemetry} or empty {@code Optional} if none match the {@param uuid}.
     */
    Optional<Telemetry> getTelemetryByUUID(UUID uuid);

    /**
     * Sends a telemetry value ({@code Telemetry}) to the core instance.
     * Will throw {@code RuntimeException} when any errors occur.
     * @param tel the {@code Telemetry} to send
     */
    void sendTelemetry(Telemetry tel);
}
