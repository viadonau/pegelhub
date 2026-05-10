package com.stm.pegelhub.outbound.data;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.exceptions.InfluxException;
import com.stm.pegelhub.common.model.data.Telemetry;
import com.stm.pegelhub.outbound.db.DatabaseProperties;
import com.stm.pegelhub.outbound.influx.ConnectionHelper;
import com.stm.pegelhub.outbound.repository.data.TelemetryRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * Influx implementation for {@code TelemetryRepository}.
 */
@Repository
public class InfluxTelemetryRepository implements TelemetryRepository {

    private final InfluxDBClient client;
    private final DatabaseProperties properties;

    public InfluxTelemetryRepository(
            @Qualifier("telemetryClient") InfluxDBClient client,
            @Qualifier("telemetryConfiguration") DatabaseProperties properties) {
        this.client = requireNonNull(client);
        this.properties = requireNonNull(properties);
    }

    /**
     * @param telemetry to save.
     * @return the saved {@link Telemetry}
     */
    @Override
    public Telemetry saveTelemetry(Telemetry telemetry) {
        Point telemetryData = new Point(telemetry.measurement());
        telemetryData.time(Instant.parse(telemetry.timestamp()), WritePrecision.MS);
        telemetryData.addTag("stationIPAddressIntern", telemetry.stationIPAddressIntern());
        telemetryData.addTag("stationIPAddressExtern", telemetry.stationIPAddressExtern());
        telemetryData.addField("cycleTime", telemetry.cycleTime());

        if(telemetry.temperatureWater() != null)
        {
            telemetryData.addField("temperatureWater", telemetry.temperatureWater());
        }

        if(telemetry.temperatureAir() != null)
        {
            telemetryData.addField("temperatureAir", telemetry.temperatureAir());
        }

        if(telemetry.performanceVoltageBattery() != null)
        {
            telemetryData.addField("performanceVoltageBattery", telemetry.performanceVoltageBattery());
        }

        if(telemetry.performanceVoltageSupply() != null)
        {
            telemetryData.addField("performanceVoltageSupply", telemetry.performanceVoltageSupply());
        }

        if(telemetry.performanceElectricityBattery() != null)
        {
            telemetryData.addField("performanceElectricityBattery", telemetry.performanceElectricityBattery());
        }

        if(telemetry.performanceElectricitySupply() != null)
        {
            telemetryData.addField("performanceElectricitySupply", telemetry.performanceElectricitySupply());
        }

        if(telemetry.fieldStrengthTransmission() != null)
        {
            telemetryData.addField("fieldStrengthTransmission", telemetry.fieldStrengthTransmission());
        }

        ConnectionHelper.writePointbyPoint(this.client, telemetryData);

        return telemetry;
    }

    /**
     * @param range in which the returned values reside.
     * @return the values from the specified range
     */
    @Override
    public List<Telemetry> getByRange(String range) {
        String query = "from(bucket: \"" + properties.bucket() + "\") |> range(start: -" + range + ")";
        HashMap<String, HashMap<String, HashMap<String, Object>>> data = ConnectionHelper.queryData(this.client, query);

        return toTelemetries(data);
    }

    /**
     * @param uuid of the desired telemetry
     * @return the corresponding {@link Telemetry} to the specified {@link UUID}
     */
    @Override
    public Telemetry getLastData(UUID uuid) {
        String query = "from(bucket: \"" + properties.bucket() + "\") |> range(start: -72h) |> filter(fn: (r) => r._measurement == \"" + uuid + "\") |> last()";

        List<Telemetry> telemetries = toTelemetries(ConnectionHelper.queryData(this.client, query));
        if (telemetries.size() == 0)
            throw new InfluxException("No telemetry found");
        return telemetries.get(0);
    }

    /**
     * @param data the data to be converted to telemetry
     * @return the converted telemetry
     */
    private List<Telemetry> toTelemetries(HashMap<String, HashMap<String, HashMap<String, Object>>> data) {
        List<Telemetry> telemetries = new ArrayList<>();
        for (Map.Entry<String, HashMap<String, HashMap<String, Object>>> measurement : data.entrySet()) {
            for (Map.Entry<String, HashMap<String, Object>> measurementEntry : measurement.getValue().entrySet()) {
                HashMap<String, Object> telemetryData = measurementEntry.getValue();
                telemetries.add(
                        new Telemetry(
                                measurement.getKey(),
                                (String) telemetryData.get("stationIPAddressIntern"),
                                (String) telemetryData.get("stationIPAddressExtern"),
                                measurementEntry.getKey(),
                                Math.toIntExact((Long) telemetryData.get("cycleTime")),
                                (Double) telemetryData.get("temperatureWater"),
                                (Double) telemetryData.get("temperatureAir"),
                                (Double) telemetryData.get("performanceVoltageBattery"),
                                (Double) telemetryData.get("performanceVoltageSupply"),
                                (Double) telemetryData.get("performanceElectricityBattery"),
                                (Double) telemetryData.get("performanceElectricitySupply"),
                                (Double) telemetryData.get("fieldStrengthTransmission")));
            }
        }
        return telemetries;
    }
}
