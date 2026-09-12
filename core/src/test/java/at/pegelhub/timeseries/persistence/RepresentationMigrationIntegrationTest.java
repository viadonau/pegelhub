package at.pegelhub.timeseries.persistence;

import at.pegelhub.testsupport.IntegrationTest;
import at.pegelhub.testsupport.PegelHubPostgresqlContainer;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.DriverManager;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@IntegrationTest
class RepresentationMigrationIntegrationTest {
    @Test
    void upgradesExistingCatalogWithoutReinterpretingSourcesAndEnforcesPropertyCompatibility() throws Exception {
        var postgres = PegelHubPostgresqlContainer.getInstance();
        postgres.start();
        String schema = "representations_" + UUID.randomUUID().toString().replace("-", "");
        Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .schemas(schema).target("1").load().migrate();

        try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            connection.setSchema(schema);
            var jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
            UUID owner = UUID.randomUUID(), station = UUID.randomUUID(), point = UUID.randomUUID(), connector = UUID.randomUUID();
            jdbc.update("insert into station_owner (id, name) values (?, 'Owner')", owner);
            jdbc.update("insert into station (id, owner_id, name, water_body) values (?, ?, 'Gauge', 'River')", station, owner);
            jdbc.update("insert into measuring_point (id, station_id, name, gauge_zero_elevation_m_above_adria) values (?, ?, 'Point', 152.68)", point, station);
            jdbc.update("insert into connector (id, name, type) values (?, 'Connector', 'iec')", connector);
            jdbc.update("insert into time_series (id, measuring_point_id, observed_property, source_connector_id, source_representation) values (?, ?, 'water-level', ?, 'metres-above-adria')", UUID.randomUUID(), point, connector);
            jdbc.update("insert into time_series (id, measuring_point_id, observed_property, source_connector_id, source_representation) values (?, ?, 'water-temperature', ?, 'canonical')", UUID.randomUUID(), point, connector);
            jdbc.update("insert into time_series (id, measuring_point_id, observed_property) values (?, ?, 'discharge')", UUID.randomUUID(), point);
            var before = jdbc.queryForList("select * from time_series order by observed_property");

            Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .schemas(schema).load().migrate();

            assertThat(jdbc.queryForList("select * from time_series order by observed_property")).isEqualTo(before);
            assertThat(jdbc.update("update time_series set source_connector_id = ?, source_representation = 'litres-per-second' where observed_property = 'discharge'", connector)).isOne();
            assertThat(jdbc.queryForObject("select source_representation from time_series where observed_property = 'discharge'", String.class))
                    .isEqualTo("litres-per-second");
            assertThatThrownBy(() -> jdbc.update("update time_series set source_representation = 'litres-per-second' where observed_property = 'water-temperature'"))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> jdbc.update("update time_series set source_representation = 'metres-above-adria' where observed_property = 'discharge'"))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> jdbc.update("update time_series set source_representation = 'unknown' where observed_property = 'discharge'"))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }
}
