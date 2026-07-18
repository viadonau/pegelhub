package at.pegelhub.telemetry.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

import static at.pegelhub.shared.validation.Validations.requirePositive;
import static java.util.Objects.requireNonNull;

/**
 * Data class for telemetry which represents an entry in the time series database (InfluxDB) in the "telemetry" (telemetry) bucket.
 */
@Schema(description = "Technical telemetry entry written by a connector.")
public record Telemetry(
        @Schema(description = "Connector identifier assigned from the authenticated client.",
                example = "a93fdc3d-b71f-44ce-a826-fe1dc1f1f357", accessMode = Schema.AccessMode.READ_ONLY)
        String measurement,
        @Schema(description = "Internal station IP address.", example = "172.16.0.10")
        String stationIPAddressIntern,
        @Schema(description = "External station IP address.", example = "203.0.113.10")
        String stationIPAddressExtern,
        @Schema(description = "Time at which the telemetry was observed.", example = "2026-06-17T12:00:00Z")
        Instant timestamp,
        @Schema(description = "Positive connector cycle time.", example = "60")
        Integer cycleTime,
        @Schema(description = "Water temperature in degrees Celsius.", example = "12.4", nullable = true)
        Double temperatureWater,
        @Schema(description = "Air temperature in degrees Celsius.", example = "18.7", nullable = true)
        Double temperatureAir,
        @Schema(description = "Positive battery voltage.", example = "12.2", nullable = true)
        Double performanceVoltageBattery,
        @Schema(description = "Positive supply voltage.", example = "24.0", nullable = true)
        Double performanceVoltageSupply,
        @Schema(description = "Positive battery current.", example = "1.2", nullable = true)
        Double performanceElectricityBattery,
        @Schema(description = "Positive supply current.", example = "0.8", nullable = true)
        Double performanceElectricitySupply,
        @Schema(description = "Positive transmission field strength.", example = "87.0", nullable = true)
        Double fieldStrengthTransmission) {
    public Telemetry {
        requireNonNull(measurement);
        requireNonNull(stationIPAddressIntern);
        requireNonNull(stationIPAddressExtern);
        requireNonNull(timestamp);
        requirePositive(cycleTime);
        if (performanceVoltageBattery != null) {
            requirePositive(performanceVoltageBattery);
        }
        if (performanceVoltageSupply != null) {
            requirePositive(performanceVoltageSupply);
        }
        if (performanceElectricityBattery != null) {
            requirePositive(performanceElectricityBattery);
        }
        if (performanceElectricitySupply != null) {
            requirePositive(performanceElectricitySupply);
        }
        if (fieldStrengthTransmission != null) {
            requirePositive(fieldStrengthTransmission);
        }
    }
}
