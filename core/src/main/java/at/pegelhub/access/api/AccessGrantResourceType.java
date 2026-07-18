package at.pegelhub.access.api;

import at.pegelhub.access.domain.AccessResourceType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AccessResourceType", description = "Resource types that can be targeted by an access grant.", enumAsRef = true)
public enum AccessGrantResourceType {
    STATION,
    TIME_SERIES;

    AccessResourceType toDomain() {
        return AccessResourceType.valueOf(name());
    }

    static AccessGrantResourceType fromDomain(AccessResourceType resourceType) {
        return AccessGrantResourceType.valueOf(resourceType.name());
    }
}
