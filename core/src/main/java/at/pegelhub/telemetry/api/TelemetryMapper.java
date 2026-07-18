package at.pegelhub.telemetry.api;

import at.pegelhub.telemetry.application.WriteTelemetryCommand;

final class TelemetryMapper {

    private TelemetryMapper() {
    }

    static WriteTelemetryCommand toCommand(WriteTelemetryRequest request) {
        return new WriteTelemetryCommand(
                request.stationIPAddressIntern(),
                request.stationIPAddressExtern(),
                request.timestamp(),
                request.cycleTime(),
                request.temperatureWater(),
                request.temperatureAir(),
                request.performanceVoltageBattery(),
                request.performanceVoltageSupply(),
                request.performanceElectricityBattery(),
                request.performanceElectricitySupply(),
                request.fieldStrengthTransmission());
    }
}
