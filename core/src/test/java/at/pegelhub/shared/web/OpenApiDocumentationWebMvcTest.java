package at.pegelhub.shared.web;

import at.pegelhub.access.application.AccessGrantService;
import at.pegelhub.connector.application.ConnectorService;
import at.pegelhub.contact.application.ContactService;
import at.pegelhub.measurement.api.read.MeasurementReadQueryResolver;
import at.pegelhub.measurement.application.MeasurementBucketResolutionPolicy;
import at.pegelhub.measurement.application.MeasurementService;
import at.pegelhub.station.application.StationService;
import at.pegelhub.stationowner.application.StationOwnerService;
import at.pegelhub.telemetry.application.TelemetryService;
import at.pegelhub.timeseries.application.TimeSeriesService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springdoc.webmvc.ui.SwaggerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration({
        SpringDocConfiguration.class,
        SpringDocConfigProperties.class,
        SpringDocWebMvcConfiguration.class,
        SwaggerConfig.class,
        SwaggerUiConfigProperties.class,
        SwaggerUiOAuthProperties.class
})
@Import({OpenApiConfiguration.class, MeasurementReadQueryResolver.class, MeasurementBucketResolutionPolicy.class})
class OpenApiDocumentationWebMvcTest {

    private static final Set<String> EXPECTED_OPERATIONS = Set.of(
            "POST /api/v1/access-grants",
            "GET /api/v1/access-grants",
            "GET /api/v1/access-grants/{id}",
            "POST /api/v1/admin/connectors",
            "POST /api/v1/connectors",
            "GET /api/v1/connectors",
            "GET /api/v1/connectors/{uuid}",
            "DELETE /api/v1/connectors/{uuid}",
            "POST /api/v1/contact",
            "GET /api/v1/contact",
            "GET /api/v1/contact/{uuid}",
            "DELETE /api/v1/contact/{uuid}",
            "POST /api/v1/measurements",
            "GET /api/v1/measurements/system-time",
            "POST /api/v1/station-owners",
            "GET /api/v1/station-owners",
            "GET /api/v1/station-owners/{id}",
            "POST /api/v1/stations",
            "GET /api/v1/stations",
            "GET /api/v1/stations/{id}",
            "POST /api/v1/telemetry",
            "GET /api/v1/telemetry/{range}",
            "GET /api/v1/telemetry/last/{uuid}",
            "POST /api/v1/time-series",
            "GET /api/v1/time-series",
            "GET /api/v1/time-series/{id}",
            "GET /api/v1/time-series/{timeSeriesId}/measurements",
            "GET /api/v1/time-series/{timeSeriesId}/measurements/buckets"
    );

    private static final Set<String> PUBLIC_OPERATIONS = Set.of(
            "GET /api/v1/measurements/system-time"
    );

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AccessGrantService accessGrantService;

    @MockitoBean
    private ConnectorService connectorService;

    @MockitoBean
    private ContactService contactService;

    @MockitoBean
    private MeasurementService measurementService;

    @MockitoBean
    private StationService stationService;

    @MockitoBean
    private StationOwnerService stationOwnerService;

    @MockitoBean
    private TelemetryService telemetryService;

    @MockitoBean
    private TimeSeriesService timeSeriesService;

    @MockitoBean
    private Clock clock;

    @BeforeEach
    void setUpClock() {
        when(clock.instant()).thenReturn(Instant.parse("2026-06-17T13:00:00Z"));
    }

    @Test
    void generatedOpenApiDocumentsEveryCurrentCoreApiOperation() throws Exception {
        JsonNode spec = openApiJson();
        JsonNode paths = spec.path("paths");

        assertThat(operationKeys(paths)).containsExactlyInAnyOrderElementsOf(EXPECTED_OPERATIONS);
        assertThat(pathNames(paths)).allMatch(path -> path.startsWith("/api/v1/"));
        assertThat(pathNames(paths)).noneMatch(path -> path.startsWith("/actuator/"));
        assertThat(pathNames(paths)).noneMatch(path -> path.startsWith("/v3/api-docs"));
        assertThat(pathNames(paths)).noneMatch(path -> path.startsWith("/swagger-ui"));

        assertThat(spec.path("info").path("title").asText()).isEqualTo("PegelHub Core API");
        assertThat(spec.path("info").path("version").asText()).isEqualTo("2.0.0-SNAPSHOT");
        assertThat(spec.path("components").path("securitySchemes").has("bearerAuth")).isTrue();
        assertThat(spec.path("components").path("responses").has("Unauthorized")).isTrue();
        assertThat(spec.path("components").path("responses").has("Forbidden")).isTrue();

        assertThat(tagNames(spec)).contains(
                "Access Grants",
                "Connector Admin",
                "Connectors",
                "Legacy Contacts",
                "Measurement Buckets",
                "Measurements",
                "Station Owners",
                "Stations",
                "Telemetry",
                "Time Series");

        assertThat(paths.path("/api/v1/measurements").path("post").path("security").isArray()).isTrue();
        assertThat(paths.path("/api/v1/measurements/system-time").path("get").has("security")).isFalse();
    }

    @Test
    void measurementReadOpenApiUsesHttpResponseSchemas() throws Exception {
        JsonNode spec = openApiJson();
        JsonNode paths = spec.path("paths");
        JsonNode schemas = spec.path("components").path("schemas");

        assertThat(schemaNames(schemas)).contains(
                "MeasurementListResponse",
                "MeasurementPointResponse",
                "MeasurementBucketListResponse",
                "MeasurementBucketPointResponse");
        assertThat(schemaNames(schemas)).doesNotContain(
                "Measurement",
                "MeasurementBucket",
                "MeasurementList",
                "MeasurementBucketList",
                "MeasurementReadQuery");

        assertThat(jsonResponseSchemaRef(paths, "/api/v1/time-series/{timeSeriesId}/measurements"))
                .isEqualTo("#/components/schemas/MeasurementListResponse");
        assertThat(jsonResponseSchemaRef(paths, "/api/v1/time-series/{timeSeriesId}/measurements/buckets"))
                .isEqualTo("#/components/schemas/MeasurementBucketListResponse");
    }

    @Test
    void accessAndConnectorOpenApiUseHttpEnumSchemas() throws Exception {
        JsonNode schemas = openApiJson().path("components").path("schemas");

        assertThat(schemaNames(schemas)).contains(
                "AccessPermission",
                "AccessResourceType",
                "ConnectorStatus");
        assertThat(schemaNames(schemas)).doesNotContain(
                "AccessGrantPermission",
                "AccessGrantResourceType",
                "ConnectorStatusDto");
    }

    @Test
    void commonAuthResponsesAreAppliedOnlyToBearerProtectedOperations() throws Exception {
        JsonNode paths = openApiJson().path("paths");

        forEachOperation(paths, (operationKey, operation) -> {
            JsonNode responses = operation.path("responses");
            if (!PUBLIC_OPERATIONS.contains(operationKey)) {
                assertThat(usesBearerAuth(operation))
                        .as("bearer security for protected operation %s", operationKey)
                        .isTrue();
                assertThat(responses.path("401").path("$ref").asText())
                        .as("401 response for %s", operationKey)
                        .isEqualTo("#/components/responses/Unauthorized");
                assertThat(responses.path("403").path("$ref").asText())
                        .as("403 response for %s", operationKey)
                        .isEqualTo("#/components/responses/Forbidden");
            } else {
                assertThat(usesBearerAuth(operation))
                        .as("bearer security for public operation %s", operationKey)
                        .isFalse();
                assertThat(responses.has("401"))
                        .as("401 response for public operation %s", operationKey)
                        .isFalse();
                assertThat(responses.has("403"))
                        .as("403 response for public operation %s", operationKey)
                        .isFalse();
            }
        });
    }

    @Test
    void measurementQueryObjectsAreDocumentedAsIndividualQueryParameters() throws Exception {
        JsonNode paths = openApiJson().path("paths");

        assertThat(queryParameterNames(paths.path("/api/v1/time-series/{timeSeriesId}/measurements").path("get")))
                .containsExactlyInAnyOrder("last", "from", "to", "order", "limit");
        assertThat(queryParameterNames(paths.path("/api/v1/time-series/{timeSeriesId}/measurements/buckets").path("get")))
                .containsExactlyInAnyOrder("last", "from", "to", "bucket", "maxPoints");
    }

    @Test
    void swaggerEndpointsAreServed() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"));

        mockMvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("openapi:")));

        int swaggerStatus = mockMvc.perform(get("/swagger-ui.html"))
                .andReturn()
                .getResponse()
                .getStatus();
        assertThat(swaggerStatus).isIn(200, 301, 302, 303, 307, 308);
    }

    private JsonNode openApiJson() throws Exception {
        String content = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(content);
    }

    private static Set<String> operationKeys(JsonNode paths) {
        Set<String> operations = new TreeSet<>();
        Iterator<Map.Entry<String, JsonNode>> fields = paths.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> path = fields.next();
            Iterator<String> methods = path.getValue().fieldNames();
            while (methods.hasNext()) {
                String method = methods.next();
                if (Set.of("get", "post", "put", "patch", "delete").contains(method)) {
                    operations.add(method.toUpperCase() + " " + path.getKey());
                }
            }
        }
        return operations;
    }

    private static void forEachOperation(JsonNode paths, BiConsumer<String, JsonNode> consumer) {
        Iterator<Map.Entry<String, JsonNode>> fields = paths.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> path = fields.next();
            Iterator<Map.Entry<String, JsonNode>> methods = path.getValue().fields();
            while (methods.hasNext()) {
                Map.Entry<String, JsonNode> method = methods.next();
                if (Set.of("get", "post", "put", "patch", "delete").contains(method.getKey())) {
                    consumer.accept(method.getKey().toUpperCase() + " " + path.getKey(), method.getValue());
                }
            }
        }
    }

    private static boolean usesBearerAuth(JsonNode operation) {
        for (JsonNode requirement : operation.path("security")) {
            if (requirement.has("bearerAuth")) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> pathNames(JsonNode paths) {
        Set<String> names = new TreeSet<>();
        paths.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private static Set<String> tagNames(JsonNode spec) {
        Set<String> names = new TreeSet<>();
        spec.path("tags").forEach(tag -> names.add(tag.path("name").asText()));
        return names;
    }

    private static Set<String> schemaNames(JsonNode schemas) {
        Set<String> names = new TreeSet<>();
        schemas.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private static String jsonResponseSchemaRef(JsonNode paths, String path) {
        JsonNode content = paths.path(path)
                .path("get")
                .path("responses")
                .path("200")
                .path("content");
        Iterator<JsonNode> mediaTypes = content.elements();
        if (!mediaTypes.hasNext()) {
            return "";
        }
        return mediaTypes.next().path("schema").path("$ref").asText();
    }

    private static Set<String> queryParameterNames(JsonNode operation) {
        Set<String> names = new TreeSet<>();
        operation.path("parameters").forEach(parameter -> {
            if ("query".equals(parameter.path("in").asText())) {
                names.add(parameter.path("name").asText());
            }
        });
        return names;
    }

}
