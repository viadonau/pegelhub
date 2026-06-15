package at.pegelhub.telemetry.persistence;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.exceptions.InfluxException;
import at.pegelhub.telemetry.domain.Telemetry;
import at.pegelhub.shared.influx.DatabaseProperties;
import at.pegelhub.shared.influx.ConnectionHelper;
import at.pegelhub.shared.influx.FluxDuration;
import at.pegelhub.shared.influx.FluxQueries;
import at.pegelhub.shared.influx.InfluxPoint;
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
    private final FluxDuration latestRange;

    public InfluxTelemetryRepository(
            @Qualifier("influxDBClient") InfluxDBClient client,
            @Qualifier("telemetryConfiguration") DatabaseProperties properties,
            @Qualifier("latestRange") FluxDuration latestRange) {
        this.client = requireNonNull(client);
        this.properties = requireNonNull(properties);
        this.latestRange = requireNonNull(latestRange);
    }

    /**
     * @param telemetry to save.
     * @return the saved {@link Telemetry}
     */
    @Override
    public Telemetry saveTelemetry(Telemetry telemetry) {
        Point telemetryData = new Point(telemetry.measurement());
        telemetryData.time(telemetry.timestamp(), WritePrecision.MS);
        telemetryData.addTag("stationIPAddressIntern", telemetry.stationIPAddressIntern());
        telemetryData.addTag("stationIPAddressExtern", telemetry.stationIPAddressExtern());
        telemetryData.addField("cycleTime", telemetry.cycleTime());

        if (telemetry.temperatureWater() != null) {
            telemetryData.addField("temperatureWater", telemetry.temperatureWater());
        }

        if (telemetry.temperatureAir() != null) {
            telemetryData.addField("temperatureAir", telemetry.temperatureAir());
        }

        if (telemetry.performanceVoltageBattery() != null) {
            telemetryData.addField("performanceVoltageBattery", telemetry.performanceVoltageBattery());
        }

        if (telemetry.performanceVoltageSupply() != null) {
            telemetryData.addField("performanceVoltageSupply", telemetry.performanceVoltageSupply());
        }

        if (telemetry.performanceElectricityBattery() != null) {
            telemetryData.addField("performanceElectricityBattery", telemetry.performanceElectricityBattery());
        }

        if (telemetry.performanceElectricitySupply() != null) {
            telemetryData.addField("performanceElectricitySupply", telemetry.performanceElectricitySupply());
        }

        if (telemetry.fieldStrengthTransmission() != null) {
            telemetryData.addField("fieldStrengthTransmission", telemetry.fieldStrengthTransmission());
        }

        ConnectionHelper.writePoint(this.client, properties, telemetryData);

        return telemetry;
    }

    /**
     * @param range in which the returned values reside.
     * @return the values from the specified range
     */
    @Override
    public List<Telemetry> getByRange(String range) {
        String query = FluxQueries.range(properties, new FluxDuration(range));
        return toTelemetries(ConnectionHelper.queryData(this.client, properties, query));
    }

    /**
     * @param uuid of the desired telemetry
     * @return the corresponding {@link Telemetry} to the specified {@link UUID}
     */
    @Override
    public Telemetry getLastData(UUID uuid) {
        String query = FluxQueries.latestMeasurement(properties, uuid, latestRange);

        List<Telemetry> telemetries = toTelemetries(ConnectionHelper.queryData(this.client, properties, query));
        if (telemetries.isEmpty())
            throw new InfluxException("No telemetry found");
        return telemetries.stream()
                .max(Comparator.comparing(Telemetry::timestamp))
                .orElseThrow(() -> new InfluxException("No telemetry found"));
    }

    /**
     * @param data the data to be converted to telemetry
     * @return the converted telemetry
     */
    private List<Telemetry> toTelemetries(List<InfluxPoint> data) {
        List<Telemetry> telemetries = new ArrayList<>();
        for (InfluxPoint point : data) {
            Instant timestamp = Optional.ofNullable(point.timestamp())
                    .orElseThrow(() -> new InfluxException("Telemetry query returned a point without a timestamp"));
            telemetries.add(
                    new Telemetry(
                            point.measurement(),
                            point.tags().get("stationIPAddressIntern"),
                            point.tags().get("stationIPAddressExtern"),
                            timestamp,
                            toInt(point.fields().get("cycleTime")),
                            toDouble(point.fields().get("temperatureWater")),
                            toDouble(point.fields().get("temperatureAir")),
                            toDouble(point.fields().get("performanceVoltageBattery")),
                            toDouble(point.fields().get("performanceVoltageSupply")),
                            toDouble(point.fields().get("performanceElectricityBattery")),
                            toDouble(point.fields().get("performanceElectricitySupply")),
                            toDouble(point.fields().get("fieldStrengthTransmission"))));
        }
        return telemetries;
    }

    private static Integer toInt(Object value) {
        return Math.toIntExact(((Number) value).longValue());
    }

    private static Double toDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }
}
