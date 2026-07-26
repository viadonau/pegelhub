package at.pegelhub.telemetry.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

@Schema(description = "openapi.telemetry.write-telemetry-request.client-supplied-technical-telemetry-values-the-authenticated")
public record WriteTelemetryRequest(
        @Schema(description = "openapi.telemetry.write-telemetry-request.internal-station-ip-address", example = "172.16.0.10")
        @NotNull String stationIPAddressIntern,
        @Schema(description = "openapi.telemetry.write-telemetry-request.external-station-ip-address", example = "203.0.113.10")
        @NotNull String stationIPAddressExtern,
        @Schema(description = "openapi.telemetry.write-telemetry-request.time-at-which-the-telemetry-was-observed", example = "2026-06-17T12:00:00Z")
        @NotNull Instant timestamp,
        @Schema(description = "openapi.telemetry.write-telemetry-request.non-negative-connector-cycle-time", example = "60")
        @NotNull @PositiveOrZero Integer cycleTime,
        @Schema(description = "openapi.telemetry.write-telemetry-request.water-temperature-in-degrees-celsius", example = "12.4", nullable = true)
        Double temperatureWater,
        @Schema(description = "openapi.telemetry.write-telemetry-request.air-temperature-in-degrees-celsius", example = "18.7", nullable = true)
        Double temperatureAir,
        @Schema(description = "openapi.telemetry.write-telemetry-request.non-negative-battery-voltage", example = "12.2", nullable = true)
        @PositiveOrZero Double performanceVoltageBattery,
        @Schema(description = "openapi.telemetry.write-telemetry-request.non-negative-supply-voltage", example = "24.0", nullable = true)
        @PositiveOrZero Double performanceVoltageSupply,
        @Schema(description = "openapi.telemetry.write-telemetry-request.non-negative-battery-current", example = "1.2", nullable = true)
        @PositiveOrZero Double performanceElectricityBattery,
        @Schema(description = "openapi.telemetry.write-telemetry-request.non-negative-supply-current", example = "0.8", nullable = true)
        @PositiveOrZero Double performanceElectricitySupply,
        @Schema(description = "openapi.telemetry.write-telemetry-request.non-negative-transmission-field-strength", example = "87.0", nullable = true)
        @PositiveOrZero Double fieldStrengthTransmission) {
}
