package at.pegelhub.access.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "openapi.access.access-permission.connector-permission-for-an-access-grant", enumAsRef = true)
public enum AccessPermission {
    READ,
    WRITE
}
