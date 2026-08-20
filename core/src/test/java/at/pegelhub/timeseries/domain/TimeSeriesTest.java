package at.pegelhub.timeseries.domain;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.shared.metadata.MetadataStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class TimeSeriesTest {

    private static final TimeSeriesId ID = new TimeSeriesId(UUID.fromString("036af782-314c-4e67-9857-c4dfe070cde3"));
    private static final MeasuringPointId POINT_ID = new MeasuringPointId(
            UUID.fromString("1acc430a-4269-414d-8cd8-4a60c7355c3a"));
    private static final ConnectorId CONNECTOR_ID = new ConnectorId(
            UUID.fromString("0cdb4ae9-20c4-4d47-bff2-cd7f03885201"));

    @Test
    void createsCanonicalSeriesWithDerivedUnit() {
        SourceAssignment source = new SourceAssignment(CONNECTOR_ID, SourceRepresentation.CANONICAL);
        TimeSeries series = TimeSeries.create(
                POINT_ID, new ObservedPropertyCode("water-level"), MetadataStatus.ACTIVE, source);

        assertThat(series.id()).isNotNull();
        assertThat(series.measuringPointId()).isEqualTo(POINT_ID);
        assertThat(series.unit()).isEqualTo("cm");
        assertThat(series.sourceConnectorId()).isEqualTo(CONNECTOR_ID);
        assertThat(series.sourceRepresentation()).isEqualTo(SourceRepresentation.CANONICAL);
    }

    @Test
    void updatePreservesImmutableIdentityFields() {
        TimeSeries series = new TimeSeries(
                ID, POINT_ID, new ObservedPropertyCode("water-temperature"), MetadataStatus.ACTIVE, null);

        TimeSeries updated = series.update(MetadataStatus.INACTIVE, null);

        assertThat(updated.id()).isEqualTo(ID);
        assertThat(updated.measuringPointId()).isEqualTo(POINT_ID);
        assertThat(updated.observedProperty().value()).isEqualTo("water-temperature");
        assertThat(updated.status()).isEqualTo(MetadataStatus.INACTIVE);
    }

    @Test
    void sourceAssignmentRequiresBothValues() {
        assertThrows(NullPointerException.class, () -> new SourceAssignment(null, SourceRepresentation.CANONICAL));
        assertThrows(NullPointerException.class, () -> new SourceAssignment(CONNECTOR_ID, null));
    }

    @Test
    void rejectsPropertyIncompatibleSourceRepresentation() {
        assertThrows(IllegalArgumentException.class, () -> new TimeSeries(
                ID,
                POINT_ID,
                new ObservedPropertyCode("water-temperature"),
                MetadataStatus.ACTIVE,
                new SourceAssignment(CONNECTOR_ID, SourceRepresentation.METRES_ABOVE_ADRIA)));
    }
}
