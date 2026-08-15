package at.pegelhub.shared.persistence;

import at.pegelhub.testsupport.IntegrationTest;
import at.pegelhub.testsupport.JpaIntegrationTestConfiguration;
import at.pegelhub.testsupport.PegelHubPostgresqlContainer;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

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
                            new Migration("2", "SQL"),
                            new Migration("3", "SQL"));
            assertMetadataRelationships(jdbc);
            assertOrphanInsertsAreRejected(jdbc);
        }
    }

    @Test
    void legacyV1SchemaIsBaselinedAtV1AndReceivesLaterMigrations() throws SQLException {
        String schema = createEmptySchema("legacy");

        createLegacyV1Schema(schema);

        try (ConfigurableApplicationContext context = baselineLegacyDatabase(schema)) {
            assertThat(context.getBean(EntityManagerFactory.class).isOpen()).isTrue();

            JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
            assertThat(migrationHistory(jdbc))
                    .containsExactly(
                            new Migration("1", "BASELINE"),
                            new Migration("2", "SQL"),
                            new Migration("3", "SQL"));
            assertMetadataRelationships(jdbc);
            assertOrphanInsertsAreRejected(jdbc);
        }
    }

    @Test
    void measuringPointMigrationBackfillsDistinctMetadataTuplesWithoutLoss() throws SQLException {
        String schema = createEmptySchema("backfill");
        migrateThrough(schema, "2");
        JdbcTemplate jdbc = jdbcTemplate(schema);

        UUID ownerId = UUID.randomUUID();
        UUID stationId = UUID.randomUUID();
        UUID firstTimeSeriesId = UUID.randomUUID();
        UUID secondTimeSeriesId = UUID.randomUUID();
        UUID distinctTimeSeriesId = UUID.randomUUID();

        jdbc.update("insert into station_owner (id, name) values (?, ?)", ownerId, "Owner");
        jdbc.update("""
                        insert into station (id, owner_id, station_number, name, water_body)
                        values (?, ?, ?, ?, ?)
                        """,
                stationId, ownerId, "backfill-station", "Kienstock", "Danube");
        insertLegacyTimeSeries(jdbc, firstTimeSeriesId, stationId, "water-level", "cm", 120.0, "right");
        insertLegacyTimeSeries(jdbc, secondTimeSeriesId, stationId, "discharge", "m3-s", 120.0, "right");
        insertLegacyTimeSeries(jdbc, distinctTimeSeriesId, stationId, "water-temperature", "celsius", 121.0, "left");

        migrateThrough(schema, "3");

        UUID firstPointId = measuringPointId(jdbc, firstTimeSeriesId);
        UUID secondPointId = measuringPointId(jdbc, secondTimeSeriesId);
        UUID distinctPointId = measuringPointId(jdbc, distinctTimeSeriesId);
        assertThat(firstPointId).isEqualTo(secondPointId).isNotEqualTo(distinctPointId);
        assertThat(jdbc.queryForObject("select count(*) from measuring_point", Integer.class)).isEqualTo(2);

        Map<String, Object> sharedMetadata = jdbc.queryForMap("""
                select measuring_point.reference_level,
                       measuring_point.reference_year,
                       measuring_point.river_kilometer,
                       measuring_point.bank,
                       measuring_point.rnw,
                       measuring_point.mw,
                       measuring_point.hsw,
                       measuring_point.hw100
                  from time_series
                  join measuring_point on measuring_point.id = time_series.measuring_point_id
                 where time_series.id = ?
                """, firstTimeSeriesId);
        assertThat(sharedMetadata)
                .containsEntry("reference_level", 120.0)
                .containsEntry("reference_year", 2010)
                .containsEntry("river_kilometer", 1921.34)
                .containsEntry("bank", "right")
                .containsEntry("rnw", 162.0)
                .containsEntry("mw", 295.0)
                .containsEntry("hsw", 480.0)
                .containsEntry("hw100", 760.0);

        List<String> timeSeriesColumns = jdbc.queryForList("""
                        select column_name
                          from information_schema.columns
                         where table_schema = current_schema()
                           and table_name = 'time_series'
                        """,
                String.class);
        assertThat(timeSeriesColumns)
                .contains("measuring_point_id", "observed_property", "unit", "external_code", "source_connector_id")
                .doesNotContain(
                        "station_id",
                        "reference_level",
                        "reference_year",
                        "river_kilometer",
                        "bank",
                        "rnw",
                        "mw",
                        "hsw",
                        "hw100");

        try (ConfigurableApplicationContext context = startFreshDatabase(schema)) {
            assertThat(context.getBean(EntityManagerFactory.class).isOpen()).isTrue();
        }
    }

    @Test
    void measuringPointBackfillIdentifiersIgnoreFloatOutputFormatting() throws SQLException {
        UUID ownerId = UUID.fromString("fb2285c8-a25a-4d63-9e56-34b044e18d3e");
        UUID stationId = UUID.fromString("5f02b90c-35e2-43cd-a67e-d035b48f0639");
        UUID timeSeriesId = UUID.fromString("d20954e8-3cde-4dbf-a8bc-e83344635ccc");

        UUID lowPrecisionId = backfilledMeasuringPointId(
                "float_low",
                "set extra_float_digits = -15",
                ownerId,
                stationId,
                timeSeriesId);
        UUID highPrecisionId = backfilledMeasuringPointId(
                "float_high",
                "set extra_float_digits = 3",
                ownerId,
                stationId,
                timeSeriesId);

        assertThat(lowPrecisionId).isEqualTo(highPrecisionId);
    }

    private static ConfigurableApplicationContext startFreshDatabase(String schema) {
        return startApplication(schema, true, false, "validate");
    }

    private static ConfigurableApplicationContext baselineLegacyDatabase(String schema) {
        return startApplication(schema, true, true, "validate");
    }

    private static void createLegacyV1Schema(String schema) {
        migrateThrough(schema, "1");
        jdbcTemplate(schema).execute("drop table flyway_schema_history");
    }

    private static void migrateThrough(String schema, String target) {
        migrateThrough(schema, target, null);
    }

    private static void migrateThrough(String schema, String target, String initSql) {
        var configuration = Flyway.configure()
                .dataSource(schemaJdbcUrl(schema), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas(schema)
                .target(target);
        if (initSql != null) {
            configuration.initSql(initSql);
        }
        configuration.load().migrate();
    }

    private static UUID backfilledMeasuringPointId(
            String scenario,
            String initSql,
            UUID ownerId,
            UUID stationId,
            UUID timeSeriesId) throws SQLException {
        String schema = createEmptySchema(scenario);
        migrateThrough(schema, "2");
        JdbcTemplate jdbc = jdbcTemplate(schema);
        jdbc.update("insert into station_owner (id, name) values (?, ?)", ownerId, "Owner");
        jdbc.update("""
                        insert into station (id, owner_id, station_number, name, water_body)
                        values (?, ?, ?, ?, ?)
                        """,
                stationId, ownerId, "float-format-station", "Kienstock", "Danube");
        insertLegacyTimeSeries(
                jdbc,
                timeSeriesId,
                stationId,
                "water-level",
                "cm",
                120.12345678901234,
                "right");

        migrateThrough(schema, "3", initSql);

        return measuringPointId(jdbc, timeSeriesId);
    }

    private static JdbcTemplate jdbcTemplate(String schema) {
        return new JdbcTemplate(new DriverManagerDataSource(
                schemaJdbcUrl(schema),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()));
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
                .filteredOn(foreignKey -> List.of("station", "measuring_point", "time_series", "access_grant")
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
                                "fk_measuring_point_station",
                                "measuring_point",
                                "station_id",
                                "station",
                                "id",
                                "RESTRICT"),
                        new ForeignKey(
                                "fk_time_series_measuring_point",
                                "time_series",
                                "measuring_point_id",
                                "measuring_point",
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
                        "ix_measuring_point_station",
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

        List<String> metadataUniqueConstraints = jdbc.queryForList("""
                        select constraint_name
                          from information_schema.table_constraints
                         where constraint_schema = current_schema()
                           and table_name in ('measuring_point', 'time_series')
                           and constraint_type = 'UNIQUE'
                        """,
                String.class);
        assertThat(metadataUniqueConstraints)
                .contains(
                        "uk_measuring_point_station_name",
                        "uk_time_series_measuring_point_property_unit")
                .doesNotContain("uk_time_series_station_property_unit");
    }

    private static void assertOrphanInsertsAreRejected(JdbcTemplate jdbc) {
        UUID ownerId = UUID.randomUUID();
        UUID stationId = UUID.randomUUID();
        UUID measuringPointId = UUID.randomUUID();

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
                        insert into measuring_point (id, station_id, name)
                        values (?, ?, ?)
                        """,
                UUID.randomUUID(), UUID.randomUUID(), "Orphan point"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_measuring_point_station");

        assertThatThrownBy(() -> jdbc.update("""
                        insert into measuring_point (id, station_id, name, bank)
                        values (?, ?, ?, ?)
                        """,
                UUID.randomUUID(), stationId, "Invalid bank", "north"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_measuring_point_bank_side");

        jdbc.update("insert into measuring_point (id, station_id, name) values (?, ?, ?)",
                measuringPointId, stationId, "Main gauge");

        assertThatThrownBy(() -> jdbc.update("""
                        insert into time_series (id, measuring_point_id, observed_property, unit)
                        values (?, ?, ?, ?)
                        """,
                UUID.randomUUID(), UUID.randomUUID(), "water-level", "cm"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_time_series_measuring_point");

        assertThat(jdbc.update("""
                        insert into time_series (id, measuring_point_id, observed_property, unit, source_connector_id)
                        values (?, ?, ?, ?, null)
                        """,
                UUID.randomUUID(), measuringPointId, "water-temperature", "celsius"))
                .isEqualTo(1);

        assertThatThrownBy(() -> jdbc.update("""
                        insert into time_series (id, measuring_point_id, observed_property, unit, source_connector_id)
                        values (?, ?, ?, ?, ?)
                        """,
                UUID.randomUUID(), measuringPointId, "discharge", "m3-s", UUID.randomUUID()))
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

    private static void insertLegacyTimeSeries(
            JdbcTemplate jdbc,
            UUID id,
            UUID stationId,
            String observedProperty,
            String unit,
            double referenceLevel,
            String bank) {
        jdbc.update("""
                        insert into time_series (
                            id,
                            station_id,
                            observed_property,
                            unit,
                            reference_level,
                            reference_year,
                            river_kilometer,
                            bank,
                            rnw,
                            mw,
                            hsw,
                            hw100)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id,
                stationId,
                observedProperty,
                unit,
                referenceLevel,
                2010,
                1921.34,
                bank,
                162.0,
                295.0,
                480.0,
                760.0);
    }

    private static UUID measuringPointId(JdbcTemplate jdbc, UUID timeSeriesId) {
        return jdbc.queryForObject(
                "select measuring_point_id from time_series where id = ?",
                UUID.class,
                timeSeriesId);
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
