package at.pegelhub.timeseries.domain;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.station.domain.StationId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class TimeSeriesTest {

    private static final TimeSeriesId ID = new TimeSeriesId(UUID.fromString("036af782-314c-4e67-9857-c4dfe070cde3"));
    private static final StationId STATION_ID = new StationId(UUID.fromString("1acc430a-4269-414d-8cd8-4a60c7355c3a"));
    private static final ConnectorId SOURCE_CONNECTOR_ID = new ConnectorId(UUID.fromString("0cdb4ae9-20c4-4d47-bff2-cd7f03885201"));
    private static final ObservedPropertyCode WATER_LEVEL = new ObservedPropertyCode("water-level");
    private static final UnitCode CENTIMETER = new UnitCode("cm");

    @Test
    void rejectsMissingRequiredValues() {
        assertThrows(NullPointerException.class,
                () -> new TimeSeries(null, STATION_ID, WATER_LEVEL, CENTIMETER, null,
                        null, null, null, null, null, null, null, null, null));
        assertThrows(NullPointerException.class,
                () -> new TimeSeries(ID, null, WATER_LEVEL, CENTIMETER, null,
                        null, null, null, null, null, null, null, null, null));
        assertThrows(NullPointerException.class,
                () -> new TimeSeries(ID, STATION_ID, null, CENTIMETER, null,
                        null, null, null, null, null, null, null, null, null));
        assertThrows(NullPointerException.class,
                () -> new TimeSeries(ID, STATION_ID, WATER_LEVEL, null, null,
                        null, null, null, null, null, null, null, null, null));
    }

    @Test
    void normalizesAndValidatesOptionalHydrologyMetadata() {
        var timeSeries = new TimeSeries(
                ID,
                STATION_ID,
                WATER_LEVEL,
                CENTIMETER,
                120.0,
                2010,
                1921.34,
                " R ",
                162.0,
                480.0,
                295.0,
                760.0,
                null,
                null);

        assertThat(timeSeries.bank()).isEqualTo("R");
        assertThat(timeSeries.referenceYear()).isEqualTo(2010);
        assertThrows(IllegalArgumentException.class, () -> new TimeSeries(
                ID,
                STATION_ID,
                WATER_LEVEL,
                CENTIMETER,
                Double.NaN,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null));
        assertThrows(IllegalArgumentException.class, () -> new TimeSeries(
                ID,
                STATION_ID,
                WATER_LEVEL,
                CENTIMETER,
                null,
                0,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null));
    }

    @Test
    void createAssignsIdentity() {
        var timeSeries = TimeSeries.create(
                STATION_ID,
                WATER_LEVEL,
                CENTIMETER,
                120.0,
                2010,
                1921.34,
                "R",
                162.0,
                480.0,
                295.0,
                760.0,
                new ExternalTimeSeriesCode("main-stage"),
                SOURCE_CONNECTOR_ID);

        assertThat(timeSeries.id()).isNotNull();
        assertThat(timeSeries.id().value()).isNotNull();
        assertThat(timeSeries.stationId()).isEqualTo(STATION_ID);
        assertThat(timeSeries.observedProperty()).isEqualTo(WATER_LEVEL);
        assertThat(timeSeries.unit()).isEqualTo(CENTIMETER);
        assertThat(timeSeries.referenceYear()).isEqualTo(2010);
        assertThat(timeSeries.riverKilometer()).isEqualTo(1921.34);
        assertThat(timeSeries.bank()).isEqualTo("R");
        assertThat(timeSeries.rnw()).isEqualTo(162.0);
        assertThat(timeSeries.hsw()).isEqualTo(480.0);
        assertThat(timeSeries.mw()).isEqualTo(295.0);
        assertThat(timeSeries.hw100()).isEqualTo(760.0);
        assertThat(timeSeries.sourceConnectorId()).isEqualTo(SOURCE_CONNECTOR_ID);
    }
}
