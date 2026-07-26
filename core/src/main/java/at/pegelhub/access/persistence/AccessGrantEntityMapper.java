package at.pegelhub.access.persistence;

import at.pegelhub.access.domain.AccessGrant;
import at.pegelhub.access.domain.AccessGrantId;
import at.pegelhub.access.domain.AccessResourceRef;
import at.pegelhub.connector.domain.ConnectorId;

final class AccessGrantEntityMapper {

    private AccessGrantEntityMapper() {
    }

    static AccessGrantEntity toEntity(AccessGrant accessGrant) {
        return new AccessGrantEntity(
                accessGrant.id().value(),
                accessGrant.connectorId().value(),
                accessGrant.resource().type(),
                accessGrant.resource().id(),
                accessGrant.permission());
    }

    static AccessGrant toDomain(AccessGrantEntity accessGrant) {
        return new AccessGrant(
                new AccessGrantId(accessGrant.id()),
                new ConnectorId(accessGrant.connectorId()),
                new AccessResourceRef(accessGrant.resourceType(), accessGrant.resourceId()),
                accessGrant.permission());
    }
}
