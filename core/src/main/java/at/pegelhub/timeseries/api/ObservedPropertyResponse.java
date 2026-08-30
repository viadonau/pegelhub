package at.pegelhub.timeseries.api;

import at.pegelhub.timeseries.domain.SourceRepresentation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "openapi.timeseries.observed-property-response.canonical-observed-property")
public record ObservedPropertyResponse(
        @Schema(description = "openapi.timeseries.observed-property-response.code", example = "water-level")
        String code,
        @Schema(description = "openapi.timeseries.observed-property-response.canonical-unit", example = "cm")
        String canonicalUnit,
        @Schema(description = "openapi.timeseries.observed-property-response.accepted-source-representations")
        List<SourceRepresentation> sourceRepresentations) { }
