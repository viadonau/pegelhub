package at.pegelhub.measurement.api;

import at.pegelhub.measurement.api.read.input.MeasurementIntervalParameters;
import at.pegelhub.measurement.api.read.output.MeasurementIntervalListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeasurementIntervalOpenApiDocumentationTest {

    @Test
    void intervalDocumentationUsesEnglishAndGermanMessageKeys() throws Exception {
        var method = MeasurementApi.class.getMethod(
                "listMeasurementIntervals", UUID.class, MeasurementIntervalParameters.class);
        var operation = method.getAnnotation(Operation.class);
        List<String> keys = new ArrayList<>();
        keys.add(operation.summary());
        keys.add(operation.description());
        for (var response : method.getAnnotation(ApiResponses.class).value()) {
            keys.add(response.description());
        }
        keys.add(method.getParameters()[0].getAnnotation(Parameter.class).description());
        addSchemaDescriptions(keys, MeasurementIntervalParameters.class);
        addSchemaDescriptions(keys, MeasurementIntervalListResponse.class);

        var english = messages("messages.properties");
        var german = messages("messages_de.properties");
        for (String key : keys) {
            assertTrue(key.startsWith("openapi."), () -> "Inline OpenAPI text: " + key);
            assertFalse(english.getProperty(key, "").isBlank(), () -> "Missing English text: " + key);
            assertFalse(german.getProperty(key, "").isBlank(), () -> "Missing German text: " + key);
            assertNotEquals(english.getProperty(key), german.getProperty(key), () -> "Untranslated text: " + key);
        }
    }

    private static void addSchemaDescriptions(List<String> keys, Class<?> type) {
        var schema = type.getAnnotation(Schema.class);
        if (schema != null) {
            keys.add(schema.description());
        }
        for (Field field : type.getDeclaredFields()) {
            schema = field.getAnnotation(Schema.class);
            if (schema != null) {
                keys.add(schema.description());
            }
        }
    }

    private static Properties messages(String resource) throws Exception {
        var properties = new Properties();
        try (var stream = MeasurementIntervalOpenApiDocumentationTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertTrue(stream != null, () -> "Missing resource: " + resource);
            properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
        return properties;
    }
}
