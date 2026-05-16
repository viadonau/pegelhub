package at.pegelhub.testsupport;

import at.pegelhub.auth.application.AuthTokenIdHolder;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.stream.Collectors;

@IntegrationTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class FullStackIntegrationTestBase {

    protected static final PegelHubPostgresqlContainer postgresqlContainer = PegelHubPostgresqlContainer.getInstance();
    protected static final PegelHubInfluxContainer influxContainer = PegelHubInfluxContainer.getInstance();

    static {
        postgresqlContainer.start();
        influxContainer.start();
    }

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    protected TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    protected void resetPostgres() {
        AuthTokenIdHolder.clear();

        List<String> tableNames = jdbcTemplate.queryForList(
                "select tablename from pg_tables where schemaname = 'public'",
                String.class);
        if (tableNames.isEmpty()) {
            return;
        }

        String tables = tableNames.stream()
                .map(tableName -> "public.\"" + tableName + "\"")
                .collect(Collectors.joining(", "));
        jdbcTemplate.execute("TRUNCATE TABLE " + tables + " RESTART IDENTITY CASCADE");
    }

    @DynamicPropertySource
    static void configureContainers(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresqlContainer::getUsername);
        registry.add("spring.datasource.password", postgresqlContainer::getPassword);
        registry.add("management.server.port", () -> "0");

        registry.add("pegelhub.influx.url", influxContainer::getUrl);
        registry.add("pegelhub.influx.org", () -> PegelHubInfluxContainer.ORG);
        registry.add("pegelhub.influx.token", () -> PegelHubInfluxContainer.ADMIN_TOKEN);
        registry.add("pegelhub.influx.data-bucket", () -> PegelHubInfluxContainer.DATA_BUCKET);
        registry.add("pegelhub.influx.telemetry-bucket", () -> PegelHubInfluxContainer.TELEMETRY_BUCKET);
    }
}
