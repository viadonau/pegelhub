package at.pegelhub.telemetry.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

import static at.pegelhub.shared.validation.Validations.requirePositive;
import static java.util.Objects.requireNonNull;

/** Technical telemetry stored for the connector identified by {@link #measurement()}. */
@Schema(description = "openapi.telemetry.telemetry.technical-telemetry-entry-written-by-a-connector")
public record Telemetry(
        @Schema(description = "openapi.telemetry.telemetry.connector-identifier-assigned-from-the-authenticated-client",
                example = "a93fdc3d-b71f-44ce-a826-fe1dc1f1f357", accessMode = Schema.AccessMode.READ_ONLY)
        String measurement,
        @Schema(description = "openapi.telemetry.write-telemetry-request.internal-station-ip-address", example = "172.16.0.10")
        String stationIPAddressIntern,
        @Schema(description = "openapi.telemetry.write-telemetry-request.external-station-ip-address", example = "203.0.113.10")
        String stationIPAddressExtern,
        @Schema(description = "openapi.telemetry.write-telemetry-request.time-at-which-the-telemetry-was-observed", example = "2026-06-17T12:00:00Z")
        Instant timestamp,
        @Schema(description = "openapi.telemetry.write-telemetry-request.non-negative-connector-cycle-time", example = "60")
        Integer cycleTime,
        @Schema(description = "openapi.telemetry.write-telemetry-request.water-temperature-in-degrees-celsius", example = "12.4", nullable = true)
        Double temperatureWater,
        @Schema(description = "openapi.telemetry.write-telemetry-request.air-temperature-in-degrees-celsius", example = "18.7", nullable = true)
        Double temperatureAir,
        @Schema(description = "openapi.telemetry.write-telemetry-request.non-negative-battery-voltage", example = "12.2", nullable = true)
        Double performanceVoltageBattery,
        @Schema(description = "openapi.telemetry.write-telemetry-request.non-negative-supply-voltage", example = "24.0", nullable = true)
        Double performanceVoltageSupply,
        @Schema(description = "openapi.telemetry.write-telemetry-request.non-negative-battery-current", example = "1.2", nullable = true)
        Double performanceElectricityBattery,
        @Schema(description = "openapi.telemetry.write-telemetry-request.non-negative-supply-current", example = "0.8", nullable = true)
        Double performanceElectricitySupply,
        @Schema(description = "openapi.telemetry.write-telemetry-request.non-negative-transmission-field-strength", example = "87.0", nullable = true)
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
