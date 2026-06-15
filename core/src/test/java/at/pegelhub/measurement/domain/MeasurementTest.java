package at.pegelhub.measurement.domain;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class MeasurementTest {

    private static final TimeSeriesId TIME_SERIES_ID = new TimeSeriesId(UUID.fromString("d55f58b1-ac1d-4a52-b1bb-7e2ec51dfb31"));
    private static final Instant OBSERVED_AT = Instant.parse("2022-02-10T10:30:00Z");
    private static final Instant RECEIVED_AT = Instant.parse("2022-02-10T10:30:01Z");
    private static final ConnectorId CONNECTOR_ID = new ConnectorId(UUID.fromString("5bf3df65-c20d-44eb-abd9-f8b54c0d9812"));

    @Test
    public void constructorWithNullArgsThrowsNPE() {
        assertThrows(NullPointerException.class, () -> new Measurement(null, OBSERVED_AT, RECEIVED_AT, 10.5, CONNECTOR_ID));
        assertThrows(NullPointerException.class, () -> new Measurement(TIME_SERIES_ID, null, RECEIVED_AT, 10.5, CONNECTOR_ID));
        assertThrows(NullPointerException.class, () -> new Measurement(TIME_SERIES_ID, OBSERVED_AT, null, 10.5, CONNECTOR_ID));
        assertThrows(NullPointerException.class, () -> new Measurement(TIME_SERIES_ID, OBSERVED_AT, RECEIVED_AT, 10.5, null));
    }

    @Test
    public void constructorWithNonFiniteValueThrowsIAE() {
        assertThrows(IllegalArgumentException.class, () -> new Measurement(TIME_SERIES_ID, OBSERVED_AT, RECEIVED_AT, Double.NaN, CONNECTOR_ID));
        assertThrows(IllegalArgumentException.class, () -> new Measurement(TIME_SERIES_ID, OBSERVED_AT, RECEIVED_AT, Double.POSITIVE_INFINITY, CONNECTOR_ID));
    }

    @Test
    public void constructorDoesNotThrow() {
        assertDoesNotThrow(() -> new Measurement(TIME_SERIES_ID, OBSERVED_AT, RECEIVED_AT, 10.5, CONNECTOR_ID));
    }
}
