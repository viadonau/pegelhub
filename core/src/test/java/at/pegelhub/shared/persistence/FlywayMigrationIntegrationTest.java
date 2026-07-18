package at.pegelhub.shared.persistence;

import at.pegelhub.testsupport.IntegrationTest;
import at.pegelhub.testsupport.JpaIntegrationTestConfiguration;
import at.pegelhub.testsupport.PegelHubPostgresqlContainer;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
final class FlywayMigrationIntegrationTest {

    private static final PegelHubPostgresqlContainer POSTGRES = PegelHubPostgresqlContainer.getInstance();

    static {
        POSTGRES.start();
    }

    @Test
    void freshDatabaseRunsEveryMigrationAndPassesHibernateValidation() throws SQLException {
        String schema = createEmptySchema("fresh");

        try (ConfigurableApplicationContext context = startFreshDatabase(schema)) {
            assertThat(context.getBean(EntityManagerFactory.class).isOpen()).isTrue();

            JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
            assertThat(migrationHistory(jdbc))
                    .containsExactly(
                            new Migration("1", "SQL"),
                            new Migration("2", "SQL"));
            assertMetadataRelationships(jdbc);
            assertOrphanInsertsAreRejected(jdbc);
        }
    }

    @Test
    void legacyHibernateSchemaIsBaselinedAtV1AndReceivesLaterMigrations() throws SQLException {
        String schema = createEmptySchema("legacy");

        try (ConfigurableApplicationContext ignored = createLegacyHibernateSchema(schema)) {
            // Closing a ddl-auto=create context leaves the legacy-equivalent schema in place.
        }

        try (ConfigurableApplicationContext context = baselineLegacyDatabase(schema)) {
            assertThat(context.getBean(EntityManagerFactory.class).isOpen()).isTrue();

            JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
            assertThat(migrationHistory(jdbc))
                    .containsExactly(
                            new Migration("1", "BASELINE"),
                            new Migration("2", "SQL"));
            assertMetadataRelationships(jdbc);
            assertOrphanInsertsAreRejected(jdbc);
        }
    }

    private static ConfigurableApplicationContext startFreshDatabase(String schema) {
        return startApplication(schema, true, false, "validate");
    }

    private static ConfigurableApplicationContext createLegacyHibernateSchema(String schema) {
        return startApplication(schema, false, false, "create");
    }

    private static ConfigurableApplicationContext baselineLegacyDatabase(String schema) {
        return startApplication(schema, true, true, "validate");
    }

    private static ConfigurableApplicationContext startApplication(
            String schema,
            boolean flywayEnabled,
            boolean baselineOnMigrate,
            String ddlAuto) {
        Map<String, Object> properties = Map.ofEntries(
                Map.entry("spring.datasource.url", schemaJdbcUrl(schema)),
                Map.entry("spring.datasource.username", POSTGRES.getUsername()),
                Map.entry("spring.datasource.password", POSTGRES.getPassword()),
                Map.entry("spring.datasource.driver-class-name", POSTGRES.getDriverClassName()),
                Map.entry("spring.flyway.enabled", flywayEnabled),
                Map.entry("spring.flyway.default-schema", schema),
                Map.entry("spring.flyway.baseline-on-migrate", baselineOnMigrate),
                Map.entry("spring.jpa.hibernate.ddl-auto", ddlAuto),
                Map.entry("spring.jpa.open-in-view", false),
                Map.entry("spring.security.oauth2.resourceserver.jwt.issuer-uri", "http://localhost/realms/test"),
                Map.entry("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", "http://localhost/jwks"));

        String[] arguments = properties.entrySet().stream()
                .map(entry -> "--" + entry.getKey() + "=" + entry.getValue())
                .toArray(String[]::new);

        return new SpringApplicationBuilder(JpaIntegrationTestConfiguration.class)
                .web(WebApplicationType.NONE)
                .run(arguments);
    }

    private static String createEmptySchema(String scenario) throws SQLException {
        String schema = "flyway_" + scenario + "_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("create schema " + schema);
        }
        return schema;
    }

    private static String schemaJdbcUrl(String schema) {
        String separator = POSTGRES.getJdbcUrl().contains("?") ? "&" : "?";
        return POSTGRES.getJdbcUrl() + separator + "currentSchema=" + schema;
    }

    private static List<Migration> migrationHistory(JdbcTemplate jdbc) {
        return jdbc.query(
                "select version, type from flyway_schema_history order by installed_rank",
                (resultSet, rowNumber) -> new Migration(
                        resultSet.getString("version"),
                        resultSet.getString("type")));
    }

    private static void assertMetadataRelationships(JdbcTemplate jdbc) {
        List<ForeignKey> foreignKeys = jdbc.query("""
                        select tc.constraint_name,
                               tc.table_name,
                               kcu.column_name,
                               ccu.table_name as referenced_table_name,
                               ccu.column_name as referenced_column_name,
                               rc.delete_rule
                          from information_schema.table_constraints tc
                          join information_schema.key_column_usage kcu
                            on kcu.constraint_schema = tc.constraint_schema
                           and kcu.constraint_name = tc.constraint_name
                          join information_schema.constraint_column_usage ccu
                            on ccu.constraint_schema = tc.constraint_schema
                           and ccu.constraint_name = tc.constraint_name
                          join information_schema.referential_constraints rc
                            on rc.constraint_schema = tc.constraint_schema
                           and rc.constraint_name = tc.constraint_name
                         where tc.constraint_schema = current_schema()
                           and tc.constraint_type = 'FOREIGN KEY'
                        """,
                (resultSet, rowNumber) -> new ForeignKey(
                        resultSet.getString("constraint_name"),
                        resultSet.getString("table_name"),
                        resultSet.getString("column_name"),
                        resultSet.getString("referenced_table_name"),
                        resultSet.getString("referenced_column_name"),
                        resultSet.getString("delete_rule")));

        assertThat(foreignKeys)
                .filteredOn(foreignKey -> List.of("station", "time_series", "access_grant")
                        .contains(foreignKey.table()))
                .containsExactlyInAnyOrder(
                        new ForeignKey(
                                "fk_station_owner",
                                "station",
                                "owner_id",
                                "station_owner",
                                "id",
                                "RESTRICT"),
                        new ForeignKey(
                                "fk_time_series_station",
                                "time_series",
                                "station_id",
                                "station",
                                "id",
                                "RESTRICT"),
                        new ForeignKey(
                                "fk_time_series_source_connector",
                                "time_series",
                                "source_connector_id",
                                "connector",
                                "id",
                                "RESTRICT"),
                        new ForeignKey(
                                "fk_access_grant_connector",
                                "access_grant",
                                "connector_id",
                                "connector",
                                "id",
                                "RESTRICT"));

        List<String> indexNames = jdbc.queryForList(
                "select indexname from pg_indexes where schemaname = current_schema()",
                String.class);
        assertThat(indexNames)
                .contains(
                        "ix_station_owner",
                        "ix_time_series_source_connector",
                        "ix_access_grant_connector",
                        "ix_access_grant_resource");

        List<String> uniqueConstraints = jdbc.queryForList("""
                        select constraint_name
                          from information_schema.table_constraints
                         where constraint_schema = current_schema()
                           and table_name = 'access_grant'
                           and constraint_type = 'UNIQUE'
                        """,
                String.class);
        assertThat(uniqueConstraints).contains("uk_access_grant_assignment");
    }

    private static void assertOrphanInsertsAreRejected(JdbcTemplate jdbc) {
        UUID ownerId = UUID.randomUUID();
        UUID stationId = UUID.randomUUID();

        assertThatThrownBy(() -> jdbc.update("""
                        insert into station (id, owner_id, station_number, name, water_body)
                        values (?, ?, ?, ?, ?)
                        """,
                UUID.randomUUID(), UUID.randomUUID(), "orphan-owner", "Station", "Danube"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_station_owner");

        jdbc.update("insert into station_owner (id, name) values (?, ?)", ownerId, "Owner");
        jdbc.update("""
                        insert into station (id, owner_id, station_number, name, water_body)
                        values (?, ?, ?, ?, ?)
                        """,
                stationId, ownerId, "valid-station", "Station", "Danube");

        assertThatThrownBy(() -> jdbc.update("""
                        insert into time_series (id, station_id, observed_property, unit)
                        values (?, ?, ?, ?)
                        """,
                UUID.randomUUID(), UUID.randomUUID(), "water-level", "cm"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_time_series_station");

        assertThat(jdbc.update("""
                        insert into time_series (id, station_id, observed_property, unit, source_connector_id)
                        values (?, ?, ?, ?, null)
                        """,
                UUID.randomUUID(), stationId, "water-temperature", "celsius"))
                .isEqualTo(1);

        assertThatThrownBy(() -> jdbc.update("""
                        insert into time_series (id, station_id, observed_property, unit, source_connector_id)
                        values (?, ?, ?, ?, ?)
                        """,
                UUID.randomUUID(), stationId, "discharge", "m3-s", UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_time_series_source_connector");

        assertThatThrownBy(() -> jdbc.update("""
                        insert into access_grant (id, connector_id, resource_type, resource_id, permission)
                        values (?, ?, ?, ?, ?)
                        """,
                UUID.randomUUID(), UUID.randomUUID(), "STATION", stationId, "READ"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_access_grant_connector");
    }

    private record Migration(String version, String type) {
    }

    private record ForeignKey(
            String name,
            String table,
            String column,
            String referencedTable,
            String referencedColumn,
            String deleteRule) {
    }
}
