package at.pegelhub.shared.web;

import at.pegelhub.access.application.AccessGrantService;
import at.pegelhub.connector.application.ConnectorService;
import at.pegelhub.contact.application.ContactService;
import at.pegelhub.measurement.api.read.MeasurementReadQueryResolver;
import at.pegelhub.measurement.application.MeasurementBucketResolutionPolicy;
import at.pegelhub.measurement.application.MeasurementService;
import at.pegelhub.measuringpoint.application.MeasuringPointService;
import at.pegelhub.station.application.StationService;
import at.pegelhub.stationowner.application.StationOwnerService;
import at.pegelhub.telemetry.application.TelemetryService;
import at.pegelhub.timeseries.application.TimeSeriesService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
        SpringDocWebMvcConfiguration.class,
        SwaggerConfig.class
})
@EnableConfigurationProperties({
        SpringDocConfigProperties.class,
        SwaggerUiConfigProperties.class,
        SwaggerUiOAuthProperties.class
})
@Import({
        OpenApiConfiguration.class,
        WebConfiguration.class,
        MeasurementReadQueryResolver.class,
        MeasurementBucketResolutionPolicy.class
})
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
            "POST /api/v1/measuring-points",
            "GET /api/v1/measuring-points",
            "GET /api/v1/measuring-points/{id}",
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

    @Autowired
    private SwaggerUiConfigProperties swaggerUiConfig;

    @Autowired
    private SpringDocConfigProperties springDocConfig;

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
    private MeasuringPointService measuringPointService;

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

        SpringDocConfigProperties configuredSpringDoc =
                bindApplicationYaml("springdoc", SpringDocConfigProperties.class);
        springDocConfig.setAllowedLocales(configuredSpringDoc.getAllowedLocales());

        SwaggerUiConfigProperties configuredSwaggerUi =
                bindApplicationYaml("springdoc.swagger-ui", SwaggerUiConfigProperties.class);
        swaggerUiConfig.setUrls(configuredSwaggerUi.getUrls());
        swaggerUiConfig.setUrlsPrimaryName(configuredSwaggerUi.getUrlsPrimaryName());
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
                "Measuring Points",
                "Station Owners",
                "Stations",
                "Telemetry",
                "Time Series");

        assertThat(paths.path("/api/v1/measurements").path("post").path("security").isArray()).isTrue();
        assertThat(paths.path("/api/v1/measurements/system-time").path("get").has("security")).isFalse();
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
    void telemetryWriteSchemaExcludesTheServerOwnedConnectorIdentity() throws Exception {
        JsonNode spec = openApiJson();
        JsonNode telemetryPost = spec.path("paths").path("/api/v1/telemetry").path("post");
        JsonNode writeSchema = spec.path("components").path("schemas").path("WriteTelemetryRequest");
        JsonNode telemetrySchema = spec.path("components").path("schemas").path("Telemetry");

        assertThat(telemetryPost.path("requestBody").path("content").path("application/json")
                .path("schema").path("$ref").asText()).endsWith("/WriteTelemetryRequest");
        assertThat(writeSchema.path("properties").has("measurement")).isFalse();
        assertThat(textValues(writeSchema.path("required")))
                .containsExactlyInAnyOrder("stationIPAddressIntern", "stationIPAddressExtern", "timestamp", "cycleTime");
        assertThat(telemetrySchema.path("properties").path("measurement").path("readOnly").asBoolean()).isTrue();
        assertThat(telemetryPost.path("responses").has("404")).isTrue();
        assertThat(spec.path("paths").path("/api/v1/telemetry/last/{uuid}").path("get")
                .path("responses").has("404")).isFalse();
        assertThat(spec.path("paths").path("/api/v1/telemetry/last/{uuid}").path("get")
                .path("responses").has("500")).isTrue();
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

    @Test
    void englishAndGermanMessageBundlesHaveTheSameKeys() throws Exception {
        assertThat(messageBundle("messages_de.properties").stringPropertyNames())
                .containsExactlyInAnyOrderElementsOf(messageBundle("messages.properties").stringPropertyNames());
    }

    @Test
    void openApiLanguageSelectionIsExplicitAndDeterministic() throws Exception {
        JsonNode defaultSpec = openApiJson();
        JsonNode englishSpec = openApiJson("en");
        JsonNode germanSpec = openApiJson("de");

        assertThat(defaultSpec.path("info").path("description").asText())
                .isEqualTo("HTTP API for PegelHub metadata, measurements, telemetry, and connector administration.");
        assertThat(englishSpec).isEqualTo(defaultSpec);
        assertThat(openApiJsonWithAcceptLanguage("de-AT")).isEqualTo(englishSpec);
        assertThat(openApiJson("en-US")).isEqualTo(englishSpec);
        assertThat(openApiJson("unsupported")).isEqualTo(englishSpec);
        assertThat(openApiJson("de-AT")).isEqualTo(germanSpec);
        assertThat(openApiJson("de-DE")).isEqualTo(germanSpec);
        assertThat(germanSpec.path("info").path("description").asText())
                .isEqualTo("HTTP API für PegelHub-Metadaten, Messwerte, Telemetrie und Konnektorverwaltung.");
    }

    @Test
    void germanOpenApiTranslatesEveryDocumentationLayerWithoutChangingTheContract() throws Exception {
        JsonNode englishSpec = openApiJson("en");
        JsonNode germanSpec = openApiJson("de");

        assertThat(unresolvedMessageKeys(englishSpec)).isEmpty();
        assertThat(unresolvedMessageKeys(germanSpec)).isEmpty();

        assertThat(tag(germanSpec, "Stations").path("description").asText()).contains("Pegelstation");
        assertThat(germanSpec.path("paths").path("/api/v1/stations").path("post").path("summary").asText())
                .contains("Pegelstation");
        assertThat(germanSpec.path("paths").path("/api/v1/stations/{id}").path("get")
                .path("parameters").path(0).path("description").asText())
                .contains("Pegelstation");
        assertThat(germanSpec.path("paths").path("/api/v1/stations").path("post")
                .path("responses").path("201").path("description").asText())
                .contains("Pegelstation");
        assertThat(germanSpec.path("components").path("schemas").path("MeasuringPointResponse")
                .path("description").asText())
                .contains("Messpunkt");
        assertThat(germanSpec.path("components").path("schemas").path("TimeSeriesResponse")
                .path("properties").path("measuringPointId").path("description").asText())
                .contains("Messpunkt");
        assertThat(germanSpec.path("components").path("responses").path("Unauthorized")
                .path("description").asText())
                .isEqualTo("Ein Bearer-Token fehlt oder ist ungültig.");
        assertThat(germanSpec.path("components").path("responses").path("Forbidden")
                .path("description").asText())
                .isEqualTo("Das authentifizierte Token gewährt nicht die erforderliche Berechtigung.");

        assertThat(jsonDifferences(
                withoutDocumentationProse(englishSpec),
                withoutDocumentationProse(germanSpec),
                "$")).isEmpty();
    }

    @Test
    void yamlDocumentationSupportsTheSameLanguageSelection() throws Exception {
        String defaultSpec = openApiYaml(null);
        String englishSpec = openApiYaml("en");
        String germanSpec = openApiYaml("de");

        assertThat(defaultSpec).contains("openapi: 3.1.0", "HTTP API for PegelHub metadata");
        assertThat(englishSpec).isEqualTo(defaultSpec);
        assertThat(openApiYaml("en-US")).isEqualTo(englishSpec);
        assertThat(openApiYaml("unsupported")).isEqualTo(englishSpec);
        assertThat(germanSpec).contains("openapi: 3.1.0", "HTTP API für PegelHub-Metadaten");
        assertThat(openApiYaml("de-AT")).isEqualTo(germanSpec);
        assertThat(openApiYaml("de-DE")).isEqualTo(germanSpec);
    }

    @Test
    void swaggerConfigurationOffersBothLanguagesAndSelectsGerman() throws Exception {
        assertThat(springDocConfig.getAllowedLocales()).containsExactly("en", "de");
        assertThat(swaggerUiConfig.getUrlsPrimaryName()).isEqualTo("Deutsch");
        String content = mockMvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode config = objectMapper.readTree(content);

        assertThat(config.path("urls.primaryName").asText()).isEqualTo("Deutsch");
        assertThat(swaggerDefinition(config, "Deutsch").path("url").asText()).isEqualTo("/v3/api-docs?lang=de");
        assertThat(swaggerDefinition(config, "English").path("url").asText()).isEqualTo("/v3/api-docs?lang=en");
    }

    private JsonNode openApiJson() throws Exception {
        String content = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(content);
    }

    private JsonNode openApiJson(String language) throws Exception {
        String content = mockMvc.perform(get("/v3/api-docs").queryParam("lang", language))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(content);
    }

    private JsonNode openApiJsonWithAcceptLanguage(String language) throws Exception {
        String content = mockMvc.perform(get("/v3/api-docs").header(HttpHeaders.ACCEPT_LANGUAGE, language))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(content);
    }

    private String openApiYaml(String language) throws Exception {
        MockHttpServletRequestBuilder request = get("/v3/api-docs.yaml");
        if (language != null) {
            request.queryParam("lang", language);
        }
        return mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private static <T> T bindApplicationYaml(String prefix, Class<T> type) {
        try {
            var applicationYaml = new FileSystemResource(
                    System.getProperty("basedir") + "/src/main/resources/application.yaml");
            MutablePropertySources propertySources = new MutablePropertySources();
            new YamlPropertySourceLoader().load("applicationYaml", applicationYaml)
                    .forEach(propertySources::addLast);

            return new Binder(ConfigurationPropertySources.from(propertySources))
                    .bind(prefix, Bindable.of(type))
                    .orElseThrow(() -> new IllegalStateException(prefix + " configuration is missing"));
        } catch (java.io.IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static Properties messageBundle(String name) throws Exception {
        Properties properties = new Properties();
        try (var stream = OpenApiDocumentationWebMvcTest.class.getClassLoader().getResourceAsStream(name)) {
            assertThat(stream).as("message bundle %s", name).isNotNull();
            properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
        return properties;
    }

    private static JsonNode tag(JsonNode spec, String name) {
        for (JsonNode tag : spec.path("tags")) {
            if (name.equals(tag.path("name").asText())) {
                return tag;
            }
        }
        throw new AssertionError("Missing OpenAPI tag: " + name);
    }

    private static JsonNode swaggerDefinition(JsonNode config, String name) {
        for (JsonNode definition : config.path("urls")) {
            if (name.equals(definition.path("name").asText())) {
                return definition;
            }
        }
        throw new AssertionError("Missing Swagger definition: " + name);
    }

    private static JsonNode withoutDocumentationProse(JsonNode spec) {
        JsonNode copy = spec.deepCopy();
        removeDocumentationProse(copy);
        return copy;
    }

    private static void removeDocumentationProse(JsonNode node) {
        if (node instanceof ObjectNode object) {
            object.remove(List.of("summary", "description"));
            object.elements().forEachRemaining(OpenApiDocumentationWebMvcTest::removeDocumentationProse);
            return;
        }
        node.elements().forEachRemaining(OpenApiDocumentationWebMvcTest::removeDocumentationProse);
    }

    private static List<String> unresolvedMessageKeys(JsonNode spec) {
        List<String> unresolved = new ArrayList<>();
        collectUnresolvedMessageKeys(spec, unresolved);
        return unresolved;
    }

    private static void collectUnresolvedMessageKeys(JsonNode node, List<String> unresolved) {
        if (node.isTextual() && node.asText().startsWith("openapi.")) {
            unresolved.add(node.asText());
        }
        node.elements().forEachRemaining(child -> collectUnresolvedMessageKeys(child, unresolved));
    }

    private static List<String> jsonDifferences(JsonNode expected, JsonNode actual, String path) {
        List<String> differences = new ArrayList<>();
        if (expected.getNodeType() != actual.getNodeType()) {
            differences.add(path + " has different node types");
            return differences;
        }
        if (expected.isObject()) {
            Set<String> expectedFields = new TreeSet<>();
            expected.fieldNames().forEachRemaining(expectedFields::add);
            Set<String> actualFields = new TreeSet<>();
            actual.fieldNames().forEachRemaining(actualFields::add);
            if (!expectedFields.equals(actualFields)) {
                differences.add(path + " has different fields: " + expectedFields + " != " + actualFields);
                return differences;
            }
            for (String field : expectedFields) {
                differences.addAll(jsonDifferences(expected.get(field), actual.get(field), path + "." + field));
            }
            return differences;
        }
        if (expected.isArray()) {
            if (expected.size() != actual.size()) {
                differences.add(path + " has different array sizes");
                return differences;
            }
            for (int index = 0; index < expected.size(); index++) {
                differences.addAll(jsonDifferences(expected.get(index), actual.get(index), path + "[" + index + "]"));
            }
            return differences;
        }
        if (!expected.equals(actual)) {
            differences.add(path + " differs: " + expected + " != " + actual);
        }
        return differences;
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

    private static Set<String> queryParameterNames(JsonNode operation) {
        Set<String> names = new TreeSet<>();
        operation.path("parameters").forEach(parameter -> {
            if ("query".equals(parameter.path("in").asText())) {
                names.add(parameter.path("name").asText());
            }
        });
        return names;
    }

    private static Set<String> textValues(JsonNode array) {
        Set<String> values = new TreeSet<>();
        array.forEach(value -> values.add(value.asText()));
        return values;
    }

}
