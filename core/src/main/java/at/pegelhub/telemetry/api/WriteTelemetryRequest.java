package at.pegelhub.telemetry.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

@Schema(description = "Client-supplied technical telemetry values. The authenticated connector identity is assigned by the server.")
public record WriteTelemetryRequest(
        @Schema(description = "Internal station IP address.", example = "172.16.0.10")
        @NotNull String stationIPAddressIntern,
        @Schema(description = "External station IP address.", example = "203.0.113.10")
        @NotNull String stationIPAddressExtern,
        @Schema(description = "Time at which the telemetry was observed.", example = "2026-06-17T12:00:00Z")
        @NotNull Instant timestamp,
        @Schema(description = "Non-negative connector cycle time.", example = "60")
        @NotNull @PositiveOrZero Integer cycleTime,
        @Schema(description = "Water temperature in degrees Celsius.", example = "12.4", nullable = true)
        Double temperatureWater,
        @Schema(description = "Air temperature in degrees Celsius.", example = "18.7", nullable = true)
        Double temperatureAir,
        @Schema(description = "Non-negative battery voltage.", example = "12.2", nullable = true)
        @PositiveOrZero Double performanceVoltageBattery,
        @Schema(description = "Non-negative supply voltage.", example = "24.0", nullable = true)
        @PositiveOrZero Double performanceVoltageSupply,
        @Schema(description = "Non-negative battery current.", example = "1.2", nullable = true)
        @PositiveOrZero Double performanceElectricityBattery,
        @Schema(description = "Non-negative supply current.", example = "0.8", nullable = true)
        @PositiveOrZero Double performanceElectricitySupply,
        @Schema(description = "Non-negative transmission field strength.", example = "87.0", nullable = true)
        @PositiveOrZero Double fieldStrengthTransmission) {
}
