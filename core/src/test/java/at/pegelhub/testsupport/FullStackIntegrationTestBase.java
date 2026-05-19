package at.pegelhub.testsupport;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.NoOpResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriTemplateHandler;

import java.net.URI;
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

    protected RestTemplate rest = new RestTemplate();

    @LocalServerPort
    private int serverPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    protected void configureRestTemplate() {
        rest.setUriTemplateHandler(new RootUriTemplateHandler("http://localhost:" + serverPort));
        rest.setErrorHandler(new NoOpResponseErrorHandler());
    }

    @BeforeEach
    protected void resetPostgres() {
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

    private record RootUriTemplateHandler(String rootUri, UriTemplateHandler delegate) implements UriTemplateHandler {

        private RootUriTemplateHandler(String rootUri) {
            this(rootUri, new DefaultUriBuilderFactory());
        }

        @Override
        public URI expand(String uriTemplate, Object... uriVariables) {
            return delegate.expand(applyRootUri(uriTemplate), uriVariables);
        }

        @Override
        public URI expand(String uriTemplate, java.util.Map<String, ?> uriVariables) {
            return delegate.expand(applyRootUri(uriTemplate), uriVariables);
        }

        private String applyRootUri(String uriTemplate) {
            if (uriTemplate.startsWith("/")) {
                return rootUri + uriTemplate;
            }
            return uriTemplate;
        }
    }
}
