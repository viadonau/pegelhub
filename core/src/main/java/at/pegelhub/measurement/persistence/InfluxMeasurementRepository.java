package at.pegelhub.measurement.persistence;

import com.influxdb.client.write.Point;
import com.influxdb.query.FluxTable;
import at.pegelhub.measurement.application.MeasurementBucketList;
import at.pegelhub.measurement.application.MeasurementBucketQuery;
import at.pegelhub.measurement.application.MeasurementList;
import at.pegelhub.measurement.application.MeasurementListQuery;
import at.pegelhub.measurement.application.MeasurementReadRow;
import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.measurement.domain.MeasurementBucket;
import at.pegelhub.shared.influx.InfluxBucketOperations;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

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
    public MeasurementList listMeasurements(MeasurementListQuery measurementQuery) {
        String query = queryBuilder.rawMeasurements(measurementQuery, measurementQuery.limit() + 1);
        List<MeasurementReadRow> rows = rowMapper.rawMeasurementRows(influx.query(query));
        boolean truncated = rows.size() > measurementQuery.limit();
        List<MeasurementReadRow> visible = truncated
                ? rows.subList(0, measurementQuery.limit())
                : rows;
        return new MeasurementList(measurementQuery, truncated, visible);
    }

    @Override
    public MeasurementBucketList listMeasurementBuckets(MeasurementBucketQuery bucketQuery) {
        Duration bucketDuration = bucketQuery.resolution().bucketWidth().duration();
        String meanBucketsQuery = queryBuilder.meanBuckets(bucketQuery);
        String countBucketsQuery = queryBuilder.countBuckets(bucketQuery);

        List<FluxTable> meanTables = influx.query(meanBucketsQuery);
        List<FluxTable> countTables = influx.query(countBucketsQuery);

        Map<MeasurementBucketKey, Double> means = rowMapper.meanRows(meanTables, bucketDuration);
        Map<MeasurementBucketKey, Long> counts = rowMapper.countRows(countTables, bucketDuration);

        List<MeasurementBucket> buckets = means.entrySet().stream()
                .filter(entry -> counts.containsKey(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new MeasurementBucket(
                        bucketQuery.timeSeriesId(),
                        entry.getKey().from(),
                        entry.getKey().to(),
                        entry.getValue(),
                        counts.get(entry.getKey())))
                .toList();
        return new MeasurementBucketList(bucketQuery, buckets);
    }

    @Override
    public Instant getSystemTime() {
        List<FluxTable> tables = influx.query(queryBuilder.systemTime());
        return rowMapper.systemTime(tables);
    }
}
