package at.pegelhub.station.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "openapi.station.create-station-request.request-to-create-station-metadata")
public record CreateStationRequest(
        @Schema(description = "openapi.station.create-station-request.station-owner-identifier", format = "uuid", example = "c45a9e77-33da-460e-ae82-dc5fc78923c5")
        @NotNull UUID ownerId,
        @Schema(description = "openapi.station.create-station-request.human-readable-station-name", example = "Korneuburg")
        @NotBlank String name,
        @Schema(description = "openapi.station.create-station-request.water-body-observed-by-the-station", example = "Danube")
        @NotBlank String waterBody) { }
