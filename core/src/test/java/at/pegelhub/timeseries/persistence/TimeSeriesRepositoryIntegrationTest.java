package at.pegelhub.timeseries.persistence;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.testsupport.JpaIntegrationTestBase;
import at.pegelhub.timeseries.domain.ExternalTimeSeriesCode;
import at.pegelhub.timeseries.domain.ObservedPropertyCode;
import at.pegelhub.timeseries.domain.TimeSeries;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import at.pegelhub.timeseries.domain.UnitCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Import(TimeSeriesRepositoryAdapter.class)
final class TimeSeriesRepositoryIntegrationTest extends JpaIntegrationTestBase {

    private static final UUID OWNER_ID = UUID.fromString("8284bd18-c39d-4047-a2af-5f0bd69da71d");
    private static final UUID CONTACT_ID = UUID.fromString("879c614b-94af-43f7-805b-448415af938b");
    private static final StationId STATION_ID = new StationId(UUID.fromString("f1bb3248-8cc0-48b8-a8a3-7f905433f98e"));
    private static final StationId OTHER_STATION_ID = new StationId(UUID.fromString("23794f24-af50-4f8a-b86f-c5733baf9de4"));
    private static final TimeSeriesId TIME_SERIES_ID = new TimeSeriesId(UUID.fromString("09f90453-b189-4a4b-a562-0be42fc55393"));
    private static final ConnectorId SOURCE_CONNECTOR_ID = new ConnectorId(UUID.fromString("380175ec-395c-4d88-8532-6d6497b1b503"));

    @Autowired
    private TimeSeriesRepository timeSeries;

    @Autowired
    private SpringDataTimeSeriesRepository springDataTimeSeries;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void insertRelationshipTargets() {
        jdbc.update("insert into station_owner (id, name) values (?, ?)", OWNER_ID, "Hydro Org");
        insertStation(STATION_ID, "station-1");
        insertStation(OTHER_STATION_ID, "station-2");

        jdbc.update("insert into contact (id) values (?)", CONTACT_ID);
        jdbc.update("""
                        insert into connector (
                            id,
                            manufacturer_id,
                            connector_number,
                            type_description,
                            software_version,
                            works_from_data_version,
                            data_definition,
                            software_manufacturer_id,
                            technically_responsible_id,
                            operating_company_id)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                SOURCE_CONNECTOR_ID.value(),
                CONTACT_ID,
                "source-connector",
                "type",
                "1.0.0",
                "1.0.0",
                "definition",
                CONTACT_ID,
                CONTACT_ID,
                CONTACT_ID);
    }

    @Test
    void savesLoadsAndFiltersTimeSeries() {
        var matching = timeSeries(TIME_SERIES_ID, STATION_ID, "water-level", "cm");
        var other = timeSeries(
                new TimeSeriesId(UUID.fromString("f5323439-e9df-4379-8375-214c5fa73c80")),
                OTHER_STATION_ID,
                "water-level",
                "cm");

        timeSeries.save(matching);
        timeSeries.save(other);

        assertThat(timeSeries.findById(TIME_SERIES_ID)).contains(matching);
        var loaded = timeSeries.findById(TIME_SERIES_ID).orElseThrow();
        assertThat(loaded.sourceConnectorId()).isEqualTo(SOURCE_CONNECTOR_ID);
        assertThat(loaded.referenceYear()).isEqualTo(2010);
        assertThat(loaded.riverKilometer()).isEqualTo(1921.34);
        assertThat(loaded.bank()).isEqualTo("R");
        assertThat(loaded.rnw()).isEqualTo(162.0);
        assertThat(loaded.hsw()).isEqualTo(480.0);
        assertThat(loaded.mw()).isEqualTo(295.0);
        assertThat(loaded.hw100()).isEqualTo(760.0);
        assertThat(timeSeries.findAll()).contains(matching, other);
        assertThat(timeSeries.findByStationId(STATION_ID)).containsExactly(matching);
    }

    @Test
    void stationPropertyUnitCombinationIsUnique() {
        timeSeries.save(timeSeries(TIME_SERIES_ID, STATION_ID, "water-level", "cm"));

        var duplicate = timeSeries(
                new TimeSeriesId(UUID.fromString("4ba678c1-67ee-4c9d-af57-03c943788778")),
                STATION_ID,
                "water-level",
                "cm");

        assertThrows(DataIntegrityViolationException.class, () -> {
            timeSeries.save(duplicate);
            springDataTimeSeries.flush();
        });
    }

    private static TimeSeries timeSeries(TimeSeriesId id, StationId stationId, String observedProperty, String unit) {
        return new TimeSeries(
                id,
                stationId,
                new ObservedPropertyCode(observedProperty),
                new UnitCode(unit),
                120.0,
                2010,
                1921.34,
                "R",
                162.0,
                480.0,
                295.0,
                760.0,
                new ExternalTimeSeriesCode("external-" + id.value()),
                SOURCE_CONNECTOR_ID);
    }

    private void insertStation(StationId id, String stationNumber) {
        jdbc.update("""
                        insert into station (id, owner_id, station_number, name, water_body)
                        values (?, ?, ?, ?, ?)
                        """,
                id.value(), OWNER_ID, stationNumber, "Station", "Danube");
    }
}
