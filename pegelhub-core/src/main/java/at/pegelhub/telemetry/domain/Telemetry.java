package at.pegelhub.telemetry.domain;

import static at.pegelhub.shared.validation.Validations.requirePositive;
import static java.util.Objects.requireNonNull;

/**
 * Data class for telemetry which represents an entry in the time series database (InfluxDB) in the "telemetry" (telemetry) bucket.
 */
public record Telemetry(String measurement, String stationIPAddressIntern, String stationIPAddressExtern,
                        String timestamp, Integer cycleTime, Double temperatureWater, Double temperatureAir,
                        Double performanceVoltageBattery, Double performanceVoltageSupply,
                        Double performanceElectricityBattery, Double performanceElectricitySupply,
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
