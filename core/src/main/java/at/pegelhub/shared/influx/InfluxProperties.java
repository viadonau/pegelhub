package at.pegelhub.shared.influx;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the shared InfluxDB connection.
 */
@ConfigurationProperties(prefix = "pegelhub.influx")
@Validated
public record InfluxProperties(
        @NotBlank String url,
        @NotBlank String org,
        @NotBlank String token,
        @NotBlank String dataBucket,
        @NotBlank String telemetryBucket,
        @NotBlank String latestRange) {

    public DatabaseProperties dataDatabase() {
        return new DatabaseProperties(url, org, dataBucket, token);
    }

    public DatabaseProperties telemetryDatabase() {
        return new DatabaseProperties(url, org, telemetryBucket, token);
    }

    public FluxDuration latestRangeDuration() {
        return new FluxDuration(latestRange);
    }
}
