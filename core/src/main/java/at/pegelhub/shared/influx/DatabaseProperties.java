package at.pegelhub.shared.influx;

import static at.pegelhub.shared.validation.Validations.requireNotEmpty;

/**
 * Properties, which are necessary to connect to a specific bucket in an InfluxDB.
 */
public record DatabaseProperties(String url, String org, String bucket, String token) {
    public DatabaseProperties {
        requireNotEmpty(url);
        requireNotEmpty(org);
        requireNotEmpty(bucket);
        requireNotEmpty(token);
    }
}
