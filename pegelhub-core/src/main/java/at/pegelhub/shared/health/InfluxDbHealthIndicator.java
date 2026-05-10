package at.pegelhub.shared.health;

import com.influxdb.client.InfluxDBClient;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.util.Assert;

/**
 * Component Class that performs the check on the InfluxDb.
 * Needs to be changed if another time series database is going to be used.
 */
public class InfluxDbHealthIndicator extends AbstractHealthIndicator {

    private final InfluxDBClient influxDbClient;

    public InfluxDbHealthIndicator(InfluxDBClient influxDbClient) {
        super("InfluxDB health check failed");
        Assert.notNull(influxDbClient, "InfluxDB client must not be null");
        this.influxDbClient = influxDbClient;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        if (influxDbClient.ping()) {
            builder.up();
        } else {
            builder.down();
        }
    }
}
