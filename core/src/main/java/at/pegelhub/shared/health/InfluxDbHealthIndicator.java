package at.pegelhub.shared.health;

import com.influxdb.client.InfluxDBClient;
import at.pegelhub.shared.influx.InfluxBucketOperations;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.util.Assert;

import java.util.List;

/**
 * Component Class that performs the check on the InfluxDb.
 * Needs to be changed if another time series database is going to be used.
 */
public class InfluxDbHealthIndicator extends AbstractHealthIndicator {

    private final InfluxDBClient influxDbClient;
    private final List<InfluxBucketOperations> buckets;

    public InfluxDbHealthIndicator(InfluxDBClient influxDbClient, List<InfluxBucketOperations> buckets) {
        super("InfluxDB health check failed");
        Assert.notNull(influxDbClient, "InfluxDB client must not be null");
        Assert.notEmpty(buckets, "At least one InfluxDB bucket must be configured");
        this.influxDbClient = influxDbClient;
        this.buckets = List.copyOf(buckets);
    }

    @Override
    protected void doHealthCheck(Health.@NonNull Builder builder) {
        if (influxDbClient.ping()) {
            for (InfluxBucketOperations bucket : buckets) {
                bucket.validateReadable();
            }
            builder.up()
                    .withDetail("buckets", buckets.stream()
                            .map(InfluxBucketOperations::bucketName)
                            .toList());
        } else {
            builder.down();
        }
    }
}
