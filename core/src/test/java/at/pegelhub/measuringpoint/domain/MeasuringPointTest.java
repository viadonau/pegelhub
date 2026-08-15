package at.pegelhub.measuringpoint.domain;

import at.pegelhub.station.domain.StationId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class MeasuringPointTest {

    private static final MeasuringPointId ID = new MeasuringPointId(
            UUID.fromString("f342472f-276f-47f5-a5e6-74216ba30c8f"));
    private static final StationId STATION_ID = new StationId(
            UUID.fromString("228136bf-331f-461d-ad5b-f0403b66b225"));

    @Test
    void rejectsMissingRequiredValues() {
        assertThrows(NullPointerException.class,
                () -> point(null, STATION_ID, "Main gauge", null, null, null, null, null, null, null, null));
        assertThrows(NullPointerException.class,
                () -> point(ID, null, "Main gauge", null, null, null, null, null, null, null, null));
        assertThrows(NullPointerException.class,
                () -> point(ID, STATION_ID, null, null, null, null, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> point(ID, STATION_ID, " ", null, null, null, null, null, null, null, null));
    }

    @Test
    void normalizesTextValues() {
        var measuringPoint = point(
                ID,
                STATION_ID,
                " Main gauge ",
                156.42,
                2020,
                1933.2,
                " left ",
                120.0,
                280.0,
                620.0,
                760.0);

        assertThat(measuringPoint.name()).isEqualTo("Main gauge");
        assertThat(measuringPoint.bank()).isEqualTo(BankSide.LEFT);
        assertThat(point(ID, STATION_ID, "Main gauge", null, null, null, " ", null, null, null, null).bank())
                .isNull();
    }

    @Test
    void rejectsInvalidReferenceYear() {
        assertThrows(IllegalArgumentException.class,
                () -> point(ID, STATION_ID, "Main gauge", null, 0, null, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> point(ID, STATION_ID, "Main gauge", null, 10000, null, null, null, null, null, null));
    }

    @Test
    void rejectsNonFiniteNumericMetadata() {
        assertThrows(IllegalArgumentException.class,
                () -> point(ID, STATION_ID, "Main gauge", Double.NaN, null, null, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> point(ID, STATION_ID, "Main gauge", null, null, Double.POSITIVE_INFINITY, null,
                        null, null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> point(ID, STATION_ID, "Main gauge", null, null, null, null,
                        Double.NEGATIVE_INFINITY, null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> point(ID, STATION_ID, "Main gauge", null, null, null, null,
                        null, Double.NaN, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> point(ID, STATION_ID, "Main gauge", null, null, null, null,
                        null, null, Double.POSITIVE_INFINITY, null));
        assertThrows(IllegalArgumentException.class,
                () -> point(ID, STATION_ID, "Main gauge", null, null, null, null,
                        null, null, null, Double.NEGATIVE_INFINITY));
    }

    @Test
    void createAssignsIdentity() {
        var measuringPoint = MeasuringPoint.create(
                STATION_ID,
                "Main gauge",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        assertThat(measuringPoint.id()).isNotNull();
        assertThat(measuringPoint.id().value()).isNotNull();
        assertThat(measuringPoint.stationId()).isEqualTo(STATION_ID);
    }

    private static MeasuringPoint point(
            MeasuringPointId id,
            StationId stationId,
            String name,
            Double referenceLevel,
            Integer referenceYear,
            Double riverKilometer,
            String bank,
            Double rnw,
            Double mw,
            Double hsw,
            Double hw100) {
        return new MeasuringPoint(
                id,
                stationId,
                name,
                referenceLevel,
                referenceYear,
                riverKilometer,
                BankSide.fromNullable(bank),
                rnw,
                mw,
                hsw,
                hw100);
    }
}
