package at.pegelhub.testsupport;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measurement.application.MeasurementReadRow;
import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.telemetry.domain.Telemetry;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.assertj.core.util.VisibleForTesting;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@VisibleForTesting
public final class ExampleData {

    private ExampleData() {
        throw new IllegalStateException("utility class can not be initialized.");
    }

    public static final UUID ID = UUID.fromString("d7305ab2-0b3d-4081-914a-e2c6047c1e12");
    public static final String MEASUREMENT_ID = "a93fdc3d-b71f-44ce-a826-fe1dc1f1f357";
    public static final String TIMESTAMP = "2010-10-12T08:50:00Z";
    public static final String IP_ADDRESS = "172.0.0.0";
    public static final int CYCLE_TIME = 1;
    public static final double TEMPERATURE = -2.0;
    public static final double PERFORMANCE = 2.0;
    public static final double FIELD_STRENGTH = 2.0;

    public static final Measurement MEASUREMENT = new Measurement(
            new TimeSeriesId(UUID.fromString(MEASUREMENT_ID)),
            Instant.parse(TIMESTAMP),
            Instant.parse(TIMESTAMP).plusSeconds(1),
            1.0,
            new ConnectorId(ID));
    public static final MeasurementReadRow MEASUREMENT_READ_ROW = new MeasurementReadRow(
            MEASUREMENT.observedAt(),
            MEASUREMENT.value(),
            MEASUREMENT.submittedByConnectorId());
    public static final List<MeasurementReadRow> MEASUREMENT_READ_ROWS = List.of(MEASUREMENT_READ_ROW);
    public static final Telemetry TELEMETRY = new Telemetry(MEASUREMENT_ID, IP_ADDRESS, IP_ADDRESS,
            Instant.parse(TIMESTAMP), CYCLE_TIME, TEMPERATURE, TEMPERATURE, PERFORMANCE, PERFORMANCE,
            PERFORMANCE, PERFORMANCE, FIELD_STRENGTH);
    public static final List<Telemetry> TELEMETRIES = List.of(TELEMETRY);

    public static final Duration REFRESH_RATE = Duration.of(100, ChronoUnit.MILLIS);
}
