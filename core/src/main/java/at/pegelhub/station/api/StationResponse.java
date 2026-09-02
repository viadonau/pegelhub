package at.pegelhub.station.api;

import at.pegelhub.shared.metadata.MetadataStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "openapi.station.station-response.station-metadata")
public record StationResponse(
        @Schema(description = "openapi.station.http-station-controller.station-identifier", format = "uuid", example = "014d58ea-fb86-4b50-bc70-ab0961736599")
        UUID id,
        @Schema(description = "openapi.station.create-station-request.station-owner-identifier", format = "uuid", example = "c45a9e77-33da-460e-ae82-dc5fc78923c5")
        UUID ownerId,
        @Schema(description = "openapi.station.create-station-request.human-readable-station-name", example = "Korneuburg")
        String name,
        @Schema(description = "openapi.station.create-station-request.water-body-observed-by-the-station", example = "Danube")
        String waterBody,
        @Schema(description = "openapi.shared.metadata-status")
        MetadataStatus status) { }
