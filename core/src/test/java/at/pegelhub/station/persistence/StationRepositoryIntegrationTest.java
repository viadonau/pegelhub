package at.pegelhub.station.persistence;

import at.pegelhub.station.domain.Station;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.stationowner.domain.StationOwnerId;
import at.pegelhub.testsupport.JpaIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Import(JpaStationRepositoryAdapter.class)
final class StationRepositoryIntegrationTest extends JpaIntegrationTestBase {

    private static final StationOwnerId OWNER_ID = new StationOwnerId(
            UUID.fromString("c109f946-6755-45a6-b940-09a5d11846ab"));
    private static final StationId STATION_ID = new StationId(
            UUID.fromString("910290a3-0fcd-4fd6-98b9-63ec5ccf21f8"));

    @Autowired
    private StationRepository stations;

    @Autowired
    private SpringDataStationRepository springDataStations;

    @Test
    void savesAndLoadsStation() {
        var station = new Station(STATION_ID, OWNER_ID, "1001", "Kienstock", "Danube", "Wachau");

        stations.save(station);

        assertThat(stations.findById(STATION_ID)).contains(station);
        assertThat(stations.findAll()).contains(station);
    }

    @Test
    void stationNumberIsUnique() {
        stations.save(new Station(STATION_ID, OWNER_ID, "1001", "Kienstock", "Danube", null));

        var duplicate = new Station(
                new StationId(UUID.fromString("1a7d4253-5791-48d5-885b-301bd42ae0be")),
                OWNER_ID,
                "1001",
                "Kienstock downstream",
                "Danube",
                null);

        assertThrows(DataIntegrityViolationException.class, () -> {
            stations.save(duplicate);
            springDataStations.flush();
        });
    }
}
