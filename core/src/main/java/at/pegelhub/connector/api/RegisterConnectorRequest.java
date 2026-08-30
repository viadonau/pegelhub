package at.pegelhub.connector.api;

import at.pegelhub.shared.metadata.MetadataStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "openapi.connector.register-connector-request.request-to-register-a-connector-and-bind")
public record RegisterConnectorRequest(
        @Schema(description = "openapi.connector.register-connector-request.keycloak-client-id-used-by-the-connector", example = "danube-ftp-import")
        @NotBlank String keycloakClientId,
        @Schema(description = "openapi.connector.register-connector-request.initial-connector-status-defaults-to-active-when", defaultValue = "active")
        MetadataStatus status,
        @Schema(description = "openapi.connector.register-connector-request.connector-metadata-to-create-and-bind")
        @Valid @NotNull CreateConnectorDto connector) {
    public MetadataStatus resolvedStatus() {
        return status == null ? MetadataStatus.ACTIVE : status;
    }
}
