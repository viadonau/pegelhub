package at.pegelhub.measuringpoint.persistence;

import at.pegelhub.measuringpoint.domain.MeasuringPoint;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.testsupport.JpaIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Import(MeasuringPointRepositoryAdapter.class)
final class MeasuringPointRepositoryIntegrationTest extends JpaIntegrationTestBase {

    private static final UUID OWNER_ID = UUID.fromString("8284bd18-c39d-4047-a2af-5f0bd69da71d");
    private static final StationId STATION_ID = new StationId(
            UUID.fromString("f1bb3248-8cc0-48b8-a8a3-7f905433f98e"));
    private static final StationId OTHER_STATION_ID = new StationId(
            UUID.fromString("23794f24-af50-4f8a-b86f-c5733baf9de4"));
    private static final MeasuringPointId MEASURING_POINT_ID = new MeasuringPointId(
            UUID.fromString("09f90453-b189-4a4b-a562-0be42fc55393"));

    @Autowired
    private MeasuringPointRepository measuringPoints;

    @Autowired
    private SpringDataMeasuringPointRepository springDataMeasuringPoints;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void insertRelationshipTargets() {
        jdbc.update("insert into station_owner (id, name) values (?, ?)", OWNER_ID, "Hydro Org");
        insertStation(STATION_ID, "station-1");
        insertStation(OTHER_STATION_ID, "station-2");
    }

    @Test
    void savesLoadsAndFiltersMeasuringPoints() {
        var matching = measuringPoint(MEASURING_POINT_ID, STATION_ID, "Main gauge");
        var other = measuringPoint(
                new MeasuringPointId(UUID.fromString("f5323439-e9df-4379-8375-214c5fa73c80")),
                OTHER_STATION_ID,
                "Main gauge");

        measuringPoints.save(matching);
        measuringPoints.save(other);

        assertThat(measuringPoints.findById(MEASURING_POINT_ID)).contains(matching);
        assertThat(measuringPoints.findAll()).contains(matching, other);
        assertThat(measuringPoints.findByStationId(STATION_ID)).containsExactly(matching);
    }

    @Test
    void nameIsUniqueWithinStation() {
        measuringPoints.save(measuringPoint(MEASURING_POINT_ID, STATION_ID, "Main gauge"));

        var duplicate = measuringPoint(
                new MeasuringPointId(UUID.fromString("4ba678c1-67ee-4c9d-af57-03c943788778")),
                STATION_ID,
                "Main gauge");

        assertThrows(DataIntegrityViolationException.class, () -> {
            measuringPoints.save(duplicate);
            springDataMeasuringPoints.flush();
        });
    }

    private static MeasuringPoint measuringPoint(MeasuringPointId id, StationId stationId, String name) {
        return new MeasuringPoint(
                id,
                stationId,
                name,
                120.0,
                2010,
                1921.34,
                "R",
                162.0,
                295.0,
                480.0,
                760.0);
    }

    private void insertStation(StationId id, String stationNumber) {
        jdbc.update("""
                        insert into station (id, owner_id, station_number, name, water_body)
                        values (?, ?, ?, ?, ?)
                        """,
                id.value(), OWNER_ID, stationNumber, "Station", "Danube");
    }
}
