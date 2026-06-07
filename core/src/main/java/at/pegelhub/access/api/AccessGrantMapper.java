package at.pegelhub.access.api;

import at.pegelhub.access.application.CreateAccessGrantCommand;
import at.pegelhub.access.domain.AccessGrant;
import at.pegelhub.access.domain.AccessResourceRef;
import at.pegelhub.access.domain.AccessResourceType;
import at.pegelhub.connector.domain.ConnectorId;

final class AccessGrantMapper {

    private AccessGrantMapper() {
    }

    static CreateAccessGrantCommand toCommand(CreateAccessGrantRequest request) {
        return new CreateAccessGrantCommand(
                new ConnectorId(request.connectorId()),
                new AccessResourceRef(request.resourceType(), request.resourceId()),
                request.permission(),
                request.validFrom(),
                request.validUntil(),
                request.includeFutureTimeSeries());
    }

    static AccessGrantResponse toResponse(AccessGrant accessGrant) {
        return new AccessGrantResponse(
                accessGrant.id().value(),
                accessGrant.connectorId().value(),
                accessGrant.resource().type(),
                accessGrant.resource().id(),
                accessGrant.permission(),
                accessGrant.validFrom(),
                accessGrant.validUntil(),
                accessGrant.includeFutureTimeSeries());
    }

    static AccessResourceRef toResourceRef(AccessResourceType resourceType, java.util.UUID resourceId) {
        return new AccessResourceRef(resourceType, resourceId);
    }
}
