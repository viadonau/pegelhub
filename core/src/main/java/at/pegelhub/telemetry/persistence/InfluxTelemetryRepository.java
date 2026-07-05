package at.pegelhub.telemetry.persistence;

import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.exceptions.InfluxException;
import at.pegelhub.shared.duration.PegelhubDurationLiteral;
import at.pegelhub.telemetry.domain.Telemetry;
import at.pegelhub.shared.influx.InfluxBucketOperations;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * Influx implementation for {@code TelemetryRepository}.
 * Telemetry is still a legacy-shaped slice; keep its Influx mapping isolated until
 * the connector runtime telemetry model is revisited.
 */
@Repository
public class InfluxTelemetryRepository implements TelemetryRepository {

    private final InfluxBucketOperations influx;
    private final PegelhubDurationLiteral latestRange;
    private final TelemetryFluxRowMapper rowMapper;
    private final TelemetryFluxQueryBuilder queryBuilder;

    public InfluxTelemetryRepository(
            @Qualifier("telemetryInfluxOperations") InfluxBucketOperations influx,
            @Qualifier("latestRange") PegelhubDurationLiteral latestRange,
            TelemetryFluxRowMapper rowMapper,
            TelemetryFluxQueryBuilder queryBuilder) {
        this.influx = requireNonNull(influx);
        this.latestRange = requireNonNull(latestRange);
        this.rowMapper = requireNonNull(rowMapper);
        this.queryBuilder = requireNonNull(queryBuilder);
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

        influx.writePoint(telemetryData);

        return telemetry;
    }

    /**
     * @param range in which the returned values reside.
     * @return the values from the specified range
     */
    @Override
    public List<Telemetry> getByRange(String range) {
        String query = queryBuilder.range(new PegelhubDurationLiteral(range));
        return rowMapper.toTelemetries(influx.query(query));
    }

    /**
     * @param uuid of the desired telemetry
     * @return the corresponding {@link Telemetry} to the specified {@link UUID}
     */
    @Override
    public Telemetry getLastData(UUID uuid) {
        String query = queryBuilder.latestTelemetry(uuid, latestRange);

        List<Telemetry> telemetries = rowMapper.toTelemetries(influx.query(query));
        if (telemetries.isEmpty())
            throw new InfluxException("No telemetry found");
        return telemetries.stream()
                .max(Comparator.comparing(Telemetry::timestamp))
                .orElseThrow(() -> new InfluxException("No telemetry found"));
    }
}
