package at.pegelhub.measurement;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measurement.application.LatestMeasurement;
import at.pegelhub.measurement.application.MeasurementReadRow;
import at.pegelhub.measurement.domain.InternalProducerId;
import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.measurement.domain.MeasurementBucket;
import at.pegelhub.measurement.domain.MeasurementValues;
import at.pegelhub.measurement.domain.WriteMeasurement;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MeasurementValuesTest {

    private static final TimeSeriesId SERIES_ID = new TimeSeriesId(UUID.randomUUID());
    private static final ConnectorId CONNECTOR_ID = new ConnectorId(UUID.randomUUID());
    private static final InternalProducerId PRODUCER_ID = new InternalProducerId(UUID.randomUUID());
    private static final Instant OBSERVED_AT = Instant.parse("2026-09-13T10:00:00Z");
    private static final Instant RECEIVED_AT = OBSERVED_AT.plusSeconds(1);

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsNonFiniteValuesAcrossReadAndWriteModels(double value) {
        assertThrows(IllegalArgumentException.class, () -> MeasurementValues.requireFinite(value));
        assertThrows(IllegalArgumentException.class, () ->
                new Measurement(SERIES_ID, OBSERVED_AT, RECEIVED_AT, value, CONNECTOR_ID));
        assertThrows(IllegalArgumentException.class, () ->
                new WriteMeasurement(SERIES_ID, OBSERVED_AT, value));
        assertThrows(IllegalArgumentException.class, () ->
                new MeasurementBucket(SERIES_ID, OBSERVED_AT, RECEIVED_AT, value, 1));
        assertThrows(IllegalArgumentException.class, () ->
                new MeasurementReadRow(OBSERVED_AT, value, CONNECTOR_ID));
        assertThrows(IllegalArgumentException.class, () ->
                new Measurement(SERIES_ID, OBSERVED_AT, RECEIVED_AT, value, null, PRODUCER_ID));
        assertThrows(IllegalArgumentException.class, () ->
                new MeasurementReadRow(OBSERVED_AT, value, null, PRODUCER_ID));
        assertThrows(IllegalArgumentException.class, () ->
                new LatestMeasurement(SERIES_ID, OBSERVED_AT, value));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-Double.MAX_VALUE, -12.5, -0.0, 0.0, Double.MIN_VALUE, 12.5, Double.MAX_VALUE})
    void preservesFiniteValuesIncludingSignedZero(double value) {
        double[] results = {
                MeasurementValues.requireFinite(value),
                new Measurement(SERIES_ID, OBSERVED_AT, RECEIVED_AT, value, CONNECTOR_ID).value(),
                new WriteMeasurement(SERIES_ID, OBSERVED_AT, value).value(),
                new MeasurementBucket(SERIES_ID, OBSERVED_AT, RECEIVED_AT, value, 1).value(),
                new MeasurementReadRow(OBSERVED_AT, value, CONNECTOR_ID).value(),
                new Measurement(SERIES_ID, OBSERVED_AT, RECEIVED_AT, value, null, PRODUCER_ID).value(),
                new MeasurementReadRow(OBSERVED_AT, value, null, PRODUCER_ID).value(),
                new LatestMeasurement(SERIES_ID, OBSERVED_AT, value).value()
        };

        for (double result : results) {
            assertEquals(Double.doubleToRawLongBits(value), Double.doubleToRawLongBits(result));
        }
    }
}
