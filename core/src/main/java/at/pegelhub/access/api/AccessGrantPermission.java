package at.pegelhub.access.api;

import at.pegelhub.access.domain.AccessPermission;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AccessPermission", description = "Connector permission for an access grant.", enumAsRef = true)
public enum AccessGrantPermission {
    READ,
    WRITE;

    AccessPermission toDomain() {
        return AccessPermission.valueOf(name());
    }

    static AccessGrantPermission fromDomain(AccessPermission permission) {
        return AccessGrantPermission.valueOf(permission.name());
    }
}
