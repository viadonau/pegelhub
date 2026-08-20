package at.pegelhub.timeseries.api;

import at.pegelhub.timeseries.domain.ObservedPropertyCatalog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/observed-properties")
@Tag(
        name = "openapi.timeseries.observed-property-controller.observed-properties",
        description = "openapi.timeseries.observed-property-controller.read-only-canonical-catalog")
@SecurityRequirement(name = "bearerAuth")
public final class ObservedPropertyController {
    @Operation(
            operationId = "listObservedProperties",
            summary = "openapi.timeseries.observed-property-controller.lists-canonical-observed-properties")
    @ApiResponse(
            responseCode = "200",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ObservedPropertyResponse.class))))
    @GetMapping
    public List<ObservedPropertyResponse> list() {
        return ObservedPropertyCatalog.definitions().stream()
                .map(definition -> new ObservedPropertyResponse(
                        definition.code(), definition.canonicalUnit(), definition.sourceRepresentations()))
                .toList();
    }
}
