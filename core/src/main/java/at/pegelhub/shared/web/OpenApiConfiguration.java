package at.pegelhub.shared.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.customizers.OpenApiLocaleCustomizer;
import org.springdoc.core.utils.PropertyResolverUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Comparator;
import java.util.Locale;

@Configuration
public class OpenApiConfiguration {

    public static final String BEARER_AUTH = "bearerAuth";
    private static final String UNAUTHORIZED = "Unauthorized";
    private static final String FORBIDDEN = "Forbidden";
    private static final String RESPONSE_REFERENCE_PREFIX = "#/components/responses/";

    @Bean
    OpenAPI pegelHubOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("PegelHub Core API")
                        .version("2.0.0-SNAPSHOT")
                        .description("openapi.info.description"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                        .addResponses(UNAUTHORIZED, new ApiResponse()
                                .description("openapi.response.unauthorized"))
                        .addResponses(FORBIDDEN, new ApiResponse()
                                .description("openapi.response.forbidden")));
    }

    @Bean
    OpenApiLocaleCustomizer localizedOpenApiDocumentation(PropertyResolverUtils properties) {
        return new OpenApiDocumentationLocalizer(properties);
    }

    @Bean
    OpenApiLocaleCustomizer deterministicOpenApiTagOrder() {
        return (openApi, ignored) -> {
            if (openApi.getTags() != null) {
                openApi.getTags().sort(Comparator.comparing(tag -> tag.getName().toLowerCase(Locale.ROOT)));
            }
        };
    }

    @Bean
    OperationCustomizer securedOperationResponses() {
        return (operation, ignored) -> {
            if (usesBearerAuth(operation)) {
                ApiResponses responses = operation.getResponses();
                if (responses == null) {
                    responses = new ApiResponses();
                    operation.setResponses(responses);
                }
                responses.putIfAbsent("401", responseReference(UNAUTHORIZED));
                responses.putIfAbsent("403", responseReference(FORBIDDEN));
            }
            return operation;
        };
    }

    private static boolean usesBearerAuth(Operation operation) {
        return operation.getSecurity() != null
                && operation.getSecurity().stream().anyMatch(requirement -> requirement.containsKey(BEARER_AUTH));
    }

    private static ApiResponse responseReference(String name) {
        return new ApiResponse().$ref(RESPONSE_REFERENCE_PREFIX + name);
    }
}
