package at.pegelhub.lib.internal.dto;

import at.pegelhub.lib.model.Telemetry;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record TelemetryCollectionReceiveDto(Map<String, Map<String, TelemetryReceiveDtoInnerType>> entriesMap) {
    public record TelemetryReceiveDtoInnerType(String stationIPAddressExtern, String stationIPAddressIntern,
        Double temperatureAir, Double temperatureWater,
        Long cycleTime,
        Double performanceElectricitySupply, Double performanceElectricityBattery,
        Double performanceVoltageSupply, Double performanceVoltageBattery,
        Double fieldStrengthTransmission) {

        public Telemetry toUnfinishedTelemetry() {
            var tel = new Telemetry();
            tel.setStationIPAddressExtern(stationIPAddressExtern);
            tel.setStationIPAddressIntern(stationIPAddressIntern);
            tel.setTemperatureAir(temperatureAir);
            tel.setTemperatureWater(temperatureWater);
            tel.setCycleTime(cycleTime);
            tel.setPerformanceElectricitySupply(performanceElectricitySupply);
            tel.setPerformanceElectricityBattery(performanceElectricityBattery);
            tel.setPerformanceVoltageSupply(performanceVoltageSupply);
            tel.setPerformanceVoltageBattery(performanceVoltageBattery);
            tel.setFieldStrengthTransmission(fieldStrengthTransmission);
            return tel;
        }
    }

    public Collection<Telemetry> toTelemetryCollection() {
        final ArrayList<Telemetry> telemetries = new ArrayList<>();

        for (var measurement : entriesMap.entrySet()) {
            var tels = measurement.getValue().entrySet().stream().map(entry -> {
                var tel = entry.getValue().toUnfinishedTelemetry();
                tel.setMeasurement(measurement.getKey());
                tel.setTimestamp(LocalDateTime.parse(entry.getKey(), DateTimeFormatter.ISO_ZONED_DATE_TIME));
                return tel;
            }).toList();
            telemetries.addAll(tels);
        }

        return telemetries;
    }
}
