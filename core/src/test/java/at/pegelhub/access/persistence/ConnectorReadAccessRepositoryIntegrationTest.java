package at.pegelhub.access.persistence;

import at.pegelhub.testsupport.JpaIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

final class ConnectorReadAccessRepositoryIntegrationTest extends JpaIntegrationTestBase {
    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID STATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CONNECTOR_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ConnectorStationReadAccessRepository stationAccess;

    @Test
    void repeatedGrantIsAtomicAndIdempotent() {
        jdbc.update("insert into station_owner (id, name) values (?, ?)", OWNER_ID, "Owner");
        jdbc.update("insert into station (id, owner_id, name, water_body) values (?, ?, ?, ?)",
                STATION_ID, OWNER_ID, "Station", "Danube");
        jdbc.update("insert into connector (id, name, type) values (?, ?, ?)",
                CONNECTOR_ID, "Connector", "other");

        assertThat(stationAccess.insertIfAbsent(CONNECTOR_ID, STATION_ID)).isEqualTo(1);
        assertThat(stationAccess.insertIfAbsent(CONNECTOR_ID, STATION_ID)).isZero();
        assertThat(stationAccess.count()).isOne();
    }
}
