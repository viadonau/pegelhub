package at.pegelhub.shared.health;

import com.influxdb.client.InfluxDBClient;
import at.pegelhub.shared.influx.ConnectionHelper;
import at.pegelhub.shared.influx.DatabaseProperties;
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
    private final List<DatabaseProperties> databases;

    public InfluxDbHealthIndicator(InfluxDBClient influxDbClient, List<DatabaseProperties> databases) {
        super("InfluxDB health check failed");
        Assert.notNull(influxDbClient, "InfluxDB client must not be null");
        Assert.notEmpty(databases, "At least one InfluxDB database must be configured");
        this.influxDbClient = influxDbClient;
        this.databases = List.copyOf(databases);
    }

    @Override
    protected void doHealthCheck(Health.@NonNull Builder builder) {
        if (influxDbClient.ping()) {
            for (DatabaseProperties database : databases) {
                ConnectionHelper.validateBucketReadable(influxDbClient, database);
            }
            builder.up()
                    .withDetail("buckets", databases.stream()
                            .map(DatabaseProperties::bucket)
                            .toList());
        } else {
            builder.down();
        }
    }
}
