package at.pegelhub.connector.api;

import at.pegelhub.connector.domain.ConnectorStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "openapi.connector.register-connector-request.request-to-register-a-connector-and-bind")
public record RegisterConnectorRequest(
        @Schema(description = "openapi.connector.register-connector-request.keycloak-client-id-used-by-the-connector", example = "local-connector-example")
        @NotBlank String keycloakClientId,
        @Schema(description = "openapi.connector.register-connector-request.initial-connector-status-defaults-to-active-when", example = "ACTIVE", nullable = true)
        ConnectorStatus status,
        @Schema(description = "openapi.connector.register-connector-request.connector-metadata-to-create-and-bind")
        @Valid @NotNull CreateConnectorDto connector
) {
    public ConnectorStatus resolvedStatus() {
        return status == null ? ConnectorStatus.ACTIVE : status;
    }
}
