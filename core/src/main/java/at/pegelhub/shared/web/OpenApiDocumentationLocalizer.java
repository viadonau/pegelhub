package at.pegelhub.shared.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiLocaleCustomizer;
import org.springdoc.core.utils.PropertyResolverUtils;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Resolves documentation keys left in parts of the generated OpenAPI model that
 * springdoc does not localize itself.
 *
 * <p>Running example:
 *
 * <pre>
 * {@code
 * @Schema(description =
 *     "openapi.measurement.measurement-bucket-point-response"
 *         + ".average-value-for-the-bucket")
 * double value
 *
 * messages_de.properties:
 * ...average-value-for-the-bucket=Durchschnittswert des Intervalls.
 *
 * OpenAPI model:
 * components
 *   -> schemas
 *   -> MeasurementBucketPointResponse
 *   -> properties
 *   -> value
 *   -> description
 * }
 * </pre>
 */
final class OpenApiDocumentationLocalizer implements OpenApiLocaleCustomizer {

    private final PropertyResolverUtils properties;

    OpenApiDocumentationLocalizer(PropertyResolverUtils properties) {
        this.properties = requireNonNull(properties);
    }

    /**
     * Starts one localization pass for a generated language.
     *
     * <p>For the running example, this receives the complete OpenAPI model and
     * starts at its reusable components.
     */
    @Override
    public void customise(OpenAPI openApi, Locale locale) {
        if (openApi.getInfo() != null) {
            openApi.getInfo().setDescription(resolve(openApi.getInfo().getDescription(), locale));
        }

        Set<Schema<?>> visitedSchemas = Collections.newSetFromMap(new IdentityHashMap<>());
        localizeComponents(openApi, locale, visitedSchemas);
        localizePathParameters(openApi, locale, visitedSchemas);
    }

    /**
     * Visits every reusable schema, response, parameter, and request body.
     *
     * <p>For the running example, this selects
     * {@code components.schemas["MeasurementBucketPointResponse"]}. Responses and
     * request bodies can contain further schemas, so those branches eventually
     * rejoin {@link #localizeSchema(Schema, Locale, Set)}.
     */
    private void localizeComponents(OpenAPI openApi, Locale locale, Set<Schema<?>> visitedSchemas) {
        if (openApi.getComponents() == null) {
            return;
        }

        values(openApi.getComponents().getSchemas()).forEach(schema -> localizeSchema(schema, locale, visitedSchemas));
        values(openApi.getComponents().getResponses()).forEach(response -> localizeResponse(response, locale, visitedSchemas));
        values(openApi.getComponents().getParameters()).forEach(parameter -> localizeParameter(parameter, locale, visitedSchemas));
        values(openApi.getComponents().getRequestBodies()).forEach(body -> localizeRequestBody(body, locale, visitedSchemas));
    }

    /**
     * Handles parameters attached directly to API paths and operations.
     *
     * <p>This is the alternative entry path for fields expanded from a
     * {@code @ParameterObject}. For example,
     * {@code MeasurementBucketParameters.last} becomes an operation parameter whose
     * description still contains a message key. It does not participate in the
     * running response-schema example.
     */
    private void localizePathParameters(OpenAPI openApi, Locale locale, Set<Schema<?>> visitedSchemas) {
        if (openApi.getPaths() == null) {
            return;
        }

        for (PathItem pathItem : openApi.getPaths().values()) {
            pathItem.readOperations().stream()
                    .flatMap(operation -> values(operation.getParameters()).stream())
                    .forEach(parameter -> localizeParameter(parameter, locale, visitedSchemas));
            values(pathItem.getParameters()).forEach(parameter -> localizeParameter(parameter, locale, visitedSchemas));
        }
    }

    /**
     * Resolves one parameter description and follows any schema attached to it.
     *
     * <p>For example, the generated {@code last} query parameter contains the
     * message key as its description. This method resolves that key and then checks
     * whether the parameter also carries a direct schema or media-type content.
     */
    private void localizeParameter(Parameter parameter, Locale locale, Set<Schema<?>> visitedSchemas) {
        if (parameter == null) {
            return;
        }

        parameter.setDescription(resolve(parameter.getDescription(), locale));
        localizeSchema(parameter.getSchema(), locale, visitedSchemas);
        localizeContent(parameter.getContent(), locale, visitedSchemas);
    }

    /**
     * Resolves a reusable request-body description and enters its payload content.
     *
     * <p>For example:
     * {@code requestBody -> application/json -> CreateStationRequest schema}.
     */
    private void localizeRequestBody(RequestBody requestBody, Locale locale, Set<Schema<?>> visitedSchemas) {
        if (requestBody == null) {
            return;
        }

        requestBody.setDescription(resolve(requestBody.getDescription(), locale));
        localizeContent(requestBody.getContent(), locale, visitedSchemas);
    }

    /**
     * Resolves a reusable response description and enters its response content.
     *
     * <p>For example:
     * {@code response -> application/json -> MeasurementBucketListResponse schema}.
     */
    private void localizeResponse(ApiResponse response, Locale locale, Set<Schema<?>> visitedSchemas) {
        if (response == null) {
            return;
        }

        response.setDescription(resolve(response.getDescription(), locale));
        localizeContent(response.getContent(), locale, visitedSchemas);
    }

    /**
     * Converts each media-type branch into a schema branch.
     *
     * <p>For example:
     * {@code application/json -> schema -> MeasurementBucketPointResponse}.
     */
    private void localizeContent(Content content, Locale locale, Set<Schema<?>> visitedSchemas) {
        for (MediaType mediaType : values(content)) {
            localizeSchema(mediaType.getSchema(), locale, visitedSchemas);
        }
    }

    /**
     * Recursively resolves schema titles and descriptions.
     *
     * <p>For the running example, this first receives
     * {@code MeasurementBucketPointResponse}, visits its {@code value} property,
     * and resolves that property's description.
     *
     * <p>Properties, array items, map values, {@code allOf}, {@code anyOf},
     * {@code oneOf}, and {@code not} are followed. Identity-based tracking prevents
     * repeated work and protects against cyclic schema graphs. A {@code $ref} target
     * is localized when that target is visited independently under
     * {@code components.schemas}.
     */
    private void localizeSchema(Schema<?> schema, Locale locale, Set<Schema<?>> visitedSchemas) {
        if (schema == null || !visitedSchemas.add(schema)) {
            return;
        }

        schema.setTitle(resolve(schema.getTitle(), locale));
        schema.setDescription(resolve(schema.getDescription(), locale));
        values(schema.getProperties()).forEach(property -> localizeSchema(property, locale, visitedSchemas));
        localizeSchema(schema.getItems(), locale, visitedSchemas);
        if (schema.getAdditionalProperties() instanceof Schema<?> additionalProperties) {
            localizeSchema(additionalProperties, locale, visitedSchemas);
        }
        values(schema.getAllOf()).forEach(nested -> localizeSchema(nested, locale, visitedSchemas));
        values(schema.getAnyOf()).forEach(nested -> localizeSchema(nested, locale, visitedSchemas));
        values(schema.getOneOf()).forEach(nested -> localizeSchema(nested, locale, visitedSchemas));
        localizeSchema(schema.getNot(), locale, visitedSchemas);
    }

    /**
     * Delegates the actual message lookup and fallback behavior to springdoc.
     *
     * <p>For the running example:
     *
     * <pre>
     * {@code
     * key:
     * openapi.measurement.measurement-bucket-point-response
     *     .average-value-for-the-bucket
     *
     * locale:
     * de
     *
     * result:
     * "Durchschnittswert des Intervalls."
     * }
     * </pre>
     */
    private String resolve(String value, Locale locale) {
        return properties.resolve(value, locale);
    }

    private static <T> List<T> values(List<T> values) {
        return values == null ? List.of() : values;
    }

    private static <K, V> List<V> values(Map<K, V> values) {
        return values == null ? List.of() : List.copyOf(values.values());
    }
}
