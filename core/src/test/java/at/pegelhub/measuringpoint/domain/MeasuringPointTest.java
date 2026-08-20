package at.pegelhub.measuringpoint.domain;

import at.pegelhub.shared.metadata.MetadataStatus;
import at.pegelhub.station.domain.StationId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class MeasuringPointTest {

    private static final MeasuringPointId ID = new MeasuringPointId(
            UUID.fromString("f342472f-276f-47f5-a5e6-74216ba30c8f"));
    private static final StationId STATION_ID = new StationId(
            UUID.fromString("228136bf-331f-461d-ad5b-f0403b66b225"));

    @Test
    void validatesIdentityAndName() {
        assertThrows(NullPointerException.class, () -> point(null, STATION_ID, "Main gauge"));
        assertThrows(NullPointerException.class, () -> point(ID, null, "Main gauge"));
        assertThrows(IllegalArgumentException.class, () -> point(ID, STATION_ID, " "));
    }

    @Test
    void preservesIdentityAndStationOnUpdate() {
        MeasuringPoint point = point(ID, STATION_ID, "Main gauge");

        MeasuringPoint updated = point.update(
                " Updated gauge ", MetadataStatus.INACTIVE,
                new MeasuringPointPosition(new BigDecimal("12.5"), BankSide.LEFT, null),
                new BigDecimal("154.22"),
                new WaterLevelReferences(2024, new BigDecimal("120"), null, new BigDecimal("280"), null));

        assertThat(updated.id()).isEqualTo(ID);
        assertThat(updated.stationId()).isEqualTo(STATION_ID);
        assertThat(updated.name()).isEqualTo("Updated gauge");
        assertThat(updated.status()).isEqualTo(MetadataStatus.INACTIVE);
        assertThat(updated.position().bank()).isEqualTo(BankSide.LEFT);
    }

    @Test
    void rejectsInvalidPositionAndReferenceSets() {
        assertThrows(IllegalArgumentException.class,
                () -> new MeasuringPointPosition(new BigDecimal("-0.1"), null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new Coordinates(new BigDecimal("91"), BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new Coordinates(BigDecimal.ZERO, new BigDecimal("181")));
        assertThrows(IllegalArgumentException.class,
                () -> new WaterLevelReferences(2024, null, null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new WaterLevelReferences(0, BigDecimal.ONE, null, null, null));
    }

    private static MeasuringPoint point(MeasuringPointId id, StationId stationId, String name) {
        return new MeasuringPoint(id, stationId, name, MetadataStatus.ACTIVE, null, null, null);
    }
}
