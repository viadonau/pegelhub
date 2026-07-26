package at.pegelhub.access.api;

import at.pegelhub.access.domain.AccessPermission;
import at.pegelhub.access.domain.AccessResourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "openapi.access.create-access-grant-request.request-to-grant-a-connector-access-to")
public record CreateAccessGrantRequest(
        @Schema(description = "openapi.access.create-access-grant-request.connector-that-receives-the-grant", format = "uuid", example = "0d9a3c87-b41a-4663-af0a-f6ec5e6a91cf")
        @NotNull
        UUID connectorId,

        @Schema(description = "openapi.access.access-grant-response.type-of-resource-the-grant-targets", example = "TIME_SERIES")
        @NotNull
        AccessResourceType resourceType,

        @Schema(description = "openapi.access.access-grant-response.identifier-of-the-station-or-time-series", format = "uuid", example = "8ce8c5b6-f093-4d46-b770-7239cdfa3d76")
        @NotNull
        UUID resourceId,

        @Schema(description = "openapi.access.access-grant-response.permission-granted-to-the-connector", example = "READ")
        @NotNull
        AccessPermission permission
) {
}
