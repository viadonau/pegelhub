package at.pegelhub.access.api;

import at.pegelhub.access.domain.AccessPermission;
import at.pegelhub.access.domain.AccessResourceType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateAccessGrantRequest(
        @NotNull
        UUID connectorId,

        @NotNull
        AccessResourceType resourceType,

        @NotNull
        UUID resourceId,

        @NotNull
        AccessPermission permission
) {
}
