package at.pegelhub.telemetry.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Request to write connector technical telemetry.")
public record TelemetryWriteRequest(
        @JsonProperty("measurement")
        @Schema(description = "Legacy telemetry identifier from older connector payloads. The write path stores the authenticated connector id.", example = "a93fdc3d-b71f-44ce-a826-fe1dc1f1f357")
        String telemetryIdentifier,
        @Schema(description = "Internal station IP address.", example = "172.16.0.10")
        String stationIPAddressIntern,
        @Schema(description = "External station IP address.", example = "203.0.113.10")
        String stationIPAddressExtern,
        @Schema(description = "Time at which the telemetry was observed.", example = "2026-06-17T12:00:00Z")
        Instant timestamp,
        @Schema(description = "Positive connector cycle time.", example = "60")
        Integer cycleTime,
        @JsonProperty("temperatureWater")
        @Schema(description = "Legacy station-observed water temperature. New integrations should write this as a TimeSeries Measurement.", example = "12.4", nullable = true, deprecated = true)
        Double legacyTemperatureWater,
        @JsonProperty("temperatureAir")
        @Schema(description = "Legacy station-observed air temperature. New integrations should write this as a TimeSeries Measurement.", example = "18.7", nullable = true, deprecated = true)
        Double legacyTemperatureAir,
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
}
