package at.pegelhub.station.domain;

import at.pegelhub.stationowner.domain.StationOwnerId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class StationTest {

    private static final StationId ID = new StationId(UUID.fromString("4acc11b3-00ff-4a06-bd89-8781d4f61e99"));
    private static final StationOwnerId OWNER_ID = new StationOwnerId(UUID.fromString("dc4331a7-cf66-4943-897f-0e8062d578d6"));

    @Test
    void rejectsMissingRequiredValues() {
        assertThrows(NullPointerException.class, () -> new Station(null, OWNER_ID, "1001", "Kienstock", "Danube", null));
        assertThrows(NullPointerException.class, () -> new Station(ID, null, "1001", "Kienstock", "Danube", null));
        assertThrows(NullPointerException.class, () -> new Station(ID, OWNER_ID, null, "Kienstock", "Danube", null));
        assertThrows(NullPointerException.class, () -> new Station(ID, OWNER_ID, "1001", null, "Danube", null));
        assertThrows(NullPointerException.class, () -> new Station(ID, OWNER_ID, "1001", "Kienstock", null, null));
    }

    @Test
    void rejectsBlankRequiredTextValues() {
        assertThrows(IllegalArgumentException.class, () -> new Station(ID, OWNER_ID, " ", "Kienstock", "Danube", null));
        assertThrows(IllegalArgumentException.class, () -> new Station(ID, OWNER_ID, "1001", " ", "Danube", null));
        assertThrows(IllegalArgumentException.class, () -> new Station(ID, OWNER_ID, "1001", "Kienstock", " ", null));
    }

    @Test
    void normalizesTextValues() {
        var station = new Station(ID, OWNER_ID, " 1001 ", " Kienstock ", " Danube ", " Wachau ");

        assertThat(station.stationNumber()).isEqualTo("1001");
        assertThat(station.name()).isEqualTo("Kienstock");
        assertThat(station.waterBody()).isEqualTo("Danube");
        assertThat(station.location()).isEqualTo("Wachau");
    }

    @Test
    void blankOptionalLocationBecomesAbsent() {
        var station = new Station(ID, OWNER_ID, "1001", "Kienstock", "Danube", " ");

        assertThat(station.location()).isNull();
    }

    @Test
    void createAssignsIdentity() {
        var station = Station.create(OWNER_ID, "1001", "Kienstock", "Danube", null);

        assertThat(station.id()).isNotNull();
        assertThat(station.id().value()).isNotNull();
    }
}
