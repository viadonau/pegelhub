package at.pegelhub.telemetry.application;

import java.time.Instant;

public record WriteTelemetryCommand(
        String stationIPAddressIntern,
        String stationIPAddressExtern,
        Instant timestamp,
        Integer cycleTime,
        Double temperatureWater,
        Double temperatureAir,
        Double performanceVoltageBattery,
        Double performanceVoltageSupply,
        Double performanceElectricityBattery,
        Double performanceElectricitySupply,
        Double fieldStrengthTransmission) {
}
