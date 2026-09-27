package at.pegelhub.measurement.persistence;

import com.influxdb.client.write.Point;
import com.influxdb.query.FluxTable;
import at.pegelhub.measurement.application.MeasurementListQuery;
import at.pegelhub.measurement.application.MeasurementReadRow;
import at.pegelhub.measurement.application.MeasurementWindow;
import at.pegelhub.measurement.application.LatestMeasurement;
import at.pegelhub.measurement.application.MeasurementLatestQuery;
import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import at.pegelhub.shared.influx.InfluxBucketOperations;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Influx implementation for {@code MeasurementRepository}.
 * Implements the storing/adding of data to the time series database.
 * Needs to be rewritten if time series database is going to be exchanged.
 */
@Repository
public class InfluxMeasurementRepository implements MeasurementRepository {

    private final InfluxBucketOperations influx;
    private final InfluxMeasurementPointMapper pointMapper;
    private final MeasurementFluxQueryBuilder queryBuilder;
    private final MeasurementFluxRowMapper rowMapper;

    InfluxMeasurementRepository(
            @Qualifier("dataInfluxOperations") InfluxBucketOperations influx,
            InfluxMeasurementPointMapper pointMapper,
            MeasurementFluxQueryBuilder queryBuilder,
            MeasurementFluxRowMapper rowMapper) {
        this.influx = requireNonNull(influx);
        this.pointMapper = requireNonNull(pointMapper);
        this.queryBuilder = requireNonNull(queryBuilder);
        this.rowMapper = requireNonNull(rowMapper);
    }

    /**
     * @param measurements to save.
     */
    @Override
    public void storeMeasurements(List<Measurement> measurements) {
        List<Point> dataPoints = pointMapper.toPoints(measurements);
        influx.writePoints(dataPoints);
    }

    @Override
    public MeasurementPage listMeasurements(MeasurementListQuery measurementQuery) {
        String query = queryBuilder.rawMeasurements(
                measurementQuery, measurementQuery.limit() + 1);
        List<MeasurementReadRow> rows = rowMapper.rawMeasurementRows(influx.query(query));
        boolean truncated = rows.size() > measurementQuery.limit();
        List<MeasurementReadRow> visible = truncated
                ? rows.subList(0, measurementQuery.limit())
                : rows;
        return new MeasurementPage(truncated, visible);
    }

    @Override
    public List<MeasurementReadRow> listIntervalEvidence(
            TimeSeriesId timeSeriesId, MeasurementWindow window, int maxRows) {
        if (maxRows < 1) {
            throw new IllegalArgumentException("maxRows must be positive");
        }
        var rows = new ArrayList<MeasurementReadRow>();
        rows.addAll(influx.queryBounded(
                        queryBuilder.intervalPredecessors(timeSeriesId, window.from()), maxRows)
                .stream().map(rowMapper::rawMeasurementRow).toList());
        // A zero remainder still runs the query so one extra row is detected instead of truncated.
        rows.addAll(influx.queryBounded(
                        queryBuilder.intervalObservations(timeSeriesId, window), maxRows - rows.size())
                .stream().map(rowMapper::rawMeasurementRow).toList());
        return List.copyOf(rows);
    }

    @Override
    public List<LatestMeasurement> listLatestMeasurements(MeasurementLatestQuery latestQuery) {
        String query = queryBuilder.latestMeasurements(latestQuery);
        return rowMapper.latestMeasurementRows(influx.query(query));
    }

    @Override
    public Instant getSystemTime() {
        List<FluxTable> tables = influx.query(queryBuilder.systemTime());
        return rowMapper.systemTime(tables);
    }
}
