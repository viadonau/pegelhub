package at.pegelhub.access.api;

import at.pegelhub.access.domain.AccessPermission;
import at.pegelhub.access.domain.AccessResourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Request to grant a connector access to a station or time series.")
public record CreateAccessGrantRequest(
        @Schema(description = "Connector that receives the grant.", format = "uuid", example = "0d9a3c87-b41a-4663-af0a-f6ec5e6a91cf")
        @NotNull
        UUID connectorId,

        @Schema(description = "Type of resource the grant targets.", example = "TIME_SERIES")
        @NotNull
        AccessResourceType resourceType,

        @Schema(description = "Identifier of the station or time series resource.", format = "uuid", example = "8ce8c5b6-f093-4d46-b770-7239cdfa3d76")
        @NotNull
        UUID resourceId,

        @Schema(description = "Permission granted to the connector.", example = "READ")
        @NotNull
        AccessPermission permission
) {
}
