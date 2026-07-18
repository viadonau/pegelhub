package at.pegelhub.telemetry.api;

import at.pegelhub.telemetry.application.WriteTelemetryCommand;
import at.pegelhub.telemetry.domain.Telemetry;

import java.util.List;

import static java.util.Objects.requireNonNull;

final class TelemetryMapper {

    private TelemetryMapper() {
    }

    static WriteTelemetryCommand toCommand(TelemetryWriteRequest request) {
        requireNonNull(request);
        return new WriteTelemetryCommand(
                request.stationIPAddressIntern(),
                request.stationIPAddressExtern(),
                request.timestamp(),
                request.cycleTime(),
                request.legacyTemperatureWater(),
                request.legacyTemperatureAir(),
                request.performanceVoltageBattery(),
                request.performanceVoltageSupply(),
                request.performanceElectricityBattery(),
                request.performanceElectricitySupply(),
                request.fieldStrengthTransmission());
    }

    static TelemetryResponse toResponse(Telemetry telemetry) {
        requireNonNull(telemetry);
        return new TelemetryResponse(
                telemetry.measurement(),
                telemetry.stationIPAddressIntern(),
                telemetry.stationIPAddressExtern(),
                telemetry.timestamp(),
                telemetry.cycleTime(),
                telemetry.legacyTemperatureWater(),
                telemetry.legacyTemperatureAir(),
                telemetry.performanceVoltageBattery(),
                telemetry.performanceVoltageSupply(),
                telemetry.performanceElectricityBattery(),
                telemetry.performanceElectricitySupply(),
                telemetry.fieldStrengthTransmission());
    }

    static List<TelemetryResponse> toResponses(List<Telemetry> telemetries) {
        requireNonNull(telemetries);
        return telemetries.stream()
                .map(TelemetryMapper::toResponse)
                .toList();
    }
}
