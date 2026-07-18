package at.pegelhub.connector.api;

import at.pegelhub.connector.domain.ConnectorStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to register a connector and bind it to a Keycloak client id.")
public record RegisterConnectorRequest(
        @Schema(description = "Keycloak client id used by the connector to authenticate.", example = "local-connector-example")
        @NotBlank String keycloakClientId,
        @Schema(description = "Initial connector status. Defaults to ACTIVE when omitted.", example = "ACTIVE", nullable = true)
        ConnectorStatusDto status,
        @Schema(description = "Connector metadata to create and bind.")
        @Valid @NotNull CreateConnectorDto connector
) {
    public ConnectorStatus resolvedStatus() {
        return status == null ? ConnectorStatus.ACTIVE : status.toDomain();
    }
}
