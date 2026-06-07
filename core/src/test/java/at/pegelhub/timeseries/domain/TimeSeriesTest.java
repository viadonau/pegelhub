package at.pegelhub.timeseries.domain;

import at.pegelhub.station.domain.StationId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class TimeSeriesTest {

    private static final TimeSeriesId ID = new TimeSeriesId(UUID.fromString("036af782-314c-4e67-9857-c4dfe070cde3"));
    private static final StationId STATION_ID = new StationId(UUID.fromString("1acc430a-4269-414d-8cd8-4a60c7355c3a"));
    private static final ObservedPropertyCode WATER_LEVEL = new ObservedPropertyCode("water-level");
    private static final UnitCode CENTIMETER = new UnitCode("cm");

    @Test
    void rejectsMissingRequiredValues() {
        assertThrows(NullPointerException.class,
                () -> new TimeSeries(null, STATION_ID, WATER_LEVEL, CENTIMETER, null, null, null));
        assertThrows(NullPointerException.class,
                () -> new TimeSeries(ID, null, WATER_LEVEL, CENTIMETER, null, null, null));
        assertThrows(NullPointerException.class,
                () -> new TimeSeries(ID, STATION_ID, null, CENTIMETER, null, null, null));
        assertThrows(NullPointerException.class,
                () -> new TimeSeries(ID, STATION_ID, WATER_LEVEL, null, null, null, null));
    }

    @Test
    void rejectsNonPositiveExpectedInterval() {
        assertThrows(IllegalArgumentException.class,
                () -> new TimeSeries(ID, STATION_ID, WATER_LEVEL, CENTIMETER, null, Duration.ZERO, null));
        assertThrows(IllegalArgumentException.class,
                () -> new TimeSeries(ID, STATION_ID, WATER_LEVEL, CENTIMETER, null, Duration.ofSeconds(-1), null));
    }

    @Test
    void createAssignsIdentity() {
        var timeSeries = TimeSeries.create(
                STATION_ID,
                WATER_LEVEL,
                CENTIMETER,
                120.0,
                Duration.ofMinutes(15),
                new ExternalTimeSeriesCode("main-stage"));

        assertThat(timeSeries.id()).isNotNull();
        assertThat(timeSeries.id().value()).isNotNull();
        assertThat(timeSeries.stationId()).isEqualTo(STATION_ID);
        assertThat(timeSeries.observedProperty()).isEqualTo(WATER_LEVEL);
        assertThat(timeSeries.unit()).isEqualTo(CENTIMETER);
    }
}
