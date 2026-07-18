package at.pegelhub.telemetry.domain;

import java.time.Instant;

import static at.pegelhub.shared.validation.Validations.requirePositive;
import static java.util.Objects.requireNonNull;

/**
 * Legacy storage/domain shape for entries in the InfluxDB telemetry bucket.
 * Runtime connector values belong here. Station-observed environmental values
 * such as water and air temperature remain only for compatibility and should
 * move to TimeSeries Measurements per ADR-0004.
 */
public record Telemetry(
        String measurement,
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

    public Double legacyTemperatureWater() {
        return temperatureWater;
    }

    public Double legacyTemperatureAir() {
        return temperatureAir;
    }
}
