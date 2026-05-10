package at.pegelhub.lib.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The model class used to send and receive {@code Telemetry} objects.
 */
public class Telemetry {
    // should be UUID but core doesn't support it yet
    private String measurement;
    private String stationIPAddressIntern;
    private String stationIPAddressExtern;
    private LocalDateTime timestamp;
    private Long cycleTime;
    private Double temperatureWater;
    private Double temperatureAir;
    private Double performanceVoltageBattery;
    private Double performanceVoltageSupply;
    private Double performanceElectricityBattery;
    private Double performanceElectricitySupply;
    private Double fieldStrengthTransmission;


    public Telemetry() {
        this.measurement = "";
        this.stationIPAddressIntern = "";
        this.stationIPAddressExtern = "";
        this.timestamp = LocalDateTime.now();
    }

    public String getMeasurement() {
        return measurement;
    }

    public void setMeasurement(String measurement) {
        this.measurement = measurement;
    }

    public String getStationIPAddressIntern() {
        return stationIPAddressIntern;
    }

    public void setStationIPAddressIntern(String stationIPAddressIntern) {
        this.stationIPAddressIntern = stationIPAddressIntern;
    }

    public String getStationIPAddressExtern() {
        return stationIPAddressExtern;
    }

    public void setStationIPAddressExtern(String stationIPAddressExtern) {
        this.stationIPAddressExtern = stationIPAddressExtern;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Long getCycleTime() {
        return cycleTime;
    }

    public void setCycleTime(Long cycleTime) {
        this.cycleTime = cycleTime;
    }

    public Double getTemperatureWater() {
        return temperatureWater;
    }

    public void setTemperatureWater(Double temperatureWater) {
        this.temperatureWater = temperatureWater;
    }

    public Double getTemperatureAir() {
        return temperatureAir;
    }

    public void setTemperatureAir(Double temperatureAir) {
        this.temperatureAir = temperatureAir;
    }

    public Double getPerformanceVoltageBattery() {
        return performanceVoltageBattery;
    }

    public void setPerformanceVoltageBattery(Double performanceVoltageBattery) {
        this.performanceVoltageBattery = performanceVoltageBattery;
    }

    public Double getPerformanceVoltageSupply() {
        return performanceVoltageSupply;
    }

    public void setPerformanceVoltageSupply(Double performanceVoltageSupply) {
        this.performanceVoltageSupply = performanceVoltageSupply;
    }

    public Double getPerformanceElectricityBattery() {
        return performanceElectricityBattery;
    }

    public void setPerformanceElectricityBattery(Double performanceElectricityBattery) {
        this.performanceElectricityBattery = performanceElectricityBattery;
    }

    public Double getPerformanceElectricitySupply() {
        return performanceElectricitySupply;
    }

    public void setPerformanceElectricitySupply(Double performanceElectricitySupply) {
        this.performanceElectricitySupply = performanceElectricitySupply;
    }

    public Double getFieldStrengthTransmission() {
        return fieldStrengthTransmission;
    }

    public void setFieldStrengthTransmission(Double fieldStrengthTransmission) {
        this.fieldStrengthTransmission = fieldStrengthTransmission;
    }

}
