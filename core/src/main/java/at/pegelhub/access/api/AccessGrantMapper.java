package at.pegelhub.access.api;

import at.pegelhub.access.application.CreateAccessGrantCommand;
import at.pegelhub.access.domain.AccessGrant;
import at.pegelhub.access.domain.AccessResourceRef;
import at.pegelhub.connector.domain.ConnectorId;

final class AccessGrantMapper {

    private AccessGrantMapper() {
    }

    static CreateAccessGrantCommand toCommand(CreateAccessGrantRequest request) {
        return new CreateAccessGrantCommand(
                new ConnectorId(request.connectorId()),
                new AccessResourceRef(request.resourceType().toDomain(), request.resourceId()),
                request.permission().toDomain());
    }

    static AccessGrantResponse toResponse(AccessGrant accessGrant) {
        return new AccessGrantResponse(
                accessGrant.id().value(),
                accessGrant.connectorId().value(),
                AccessGrantResourceType.fromDomain(accessGrant.resource().type()),
                accessGrant.resource().id(),
                AccessGrantPermission.fromDomain(accessGrant.permission()));
    }
}
