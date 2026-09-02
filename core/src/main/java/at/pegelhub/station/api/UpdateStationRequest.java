package at.pegelhub.station.api;

import at.pegelhub.shared.metadata.MetadataStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "openapi.station.update-station-request.request-to-replace-station-metadata")
public record UpdateStationRequest(
        @Schema(description = "openapi.station.create-station-request.human-readable-station-name", example = "Korneuburg")
        @NotBlank String name,
        @Schema(description = "openapi.station.create-station-request.water-body-observed-by-the-station", example = "Danube")
        @NotBlank String waterBody,
        @Schema(description = "openapi.shared.metadata-status")
        @NotNull MetadataStatus status) { }
