package at.pegelhub.connector.api;

import at.pegelhub.connector.domain.ConnectorStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterConnectorRequest(
        @NotBlank String keycloakClientId,
        ConnectorStatus status,
        @Valid @NotNull CreateConnectorDto connector
) {
    public ConnectorStatus resolvedStatus() {
        return status == null ? ConnectorStatus.ACTIVE : status;
    }
}
