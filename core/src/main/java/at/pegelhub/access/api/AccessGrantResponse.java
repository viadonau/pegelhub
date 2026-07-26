package at.pegelhub.access.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Access grant assigned to a connector.")
public record AccessGrantResponse(
        @Schema(description = "Access grant identifier.", format = "uuid", example = "6f0c1b42-8a6d-4a25-b4ed-df80f29b29b1")
        UUID id,
        @Schema(description = "Connector that owns the grant.", format = "uuid", example = "0d9a3c87-b41a-4663-af0a-f6ec5e6a91cf")
        UUID connectorId,
        @Schema(description = "Type of resource the grant targets.", example = "TIME_SERIES")
        AccessGrantResourceType resourceType,
        @Schema(description = "Identifier of the station or time series resource.", format = "uuid", example = "8ce8c5b6-f093-4d46-b770-7239cdfa3d76")
        UUID resourceId,
        @Schema(description = "Permission granted to the connector.", example = "READ")
        AccessGrantPermission permission
) {
}
