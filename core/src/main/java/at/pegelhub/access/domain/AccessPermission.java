package at.pegelhub.access.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Connector permission for an access grant.", enumAsRef = true)
public enum AccessPermission {
    READ,
    WRITE
}
