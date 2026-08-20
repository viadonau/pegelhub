package at.pegelhub.connector.api;

import at.pegelhub.shared.metadata.MetadataStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterConnectorRequest(
        @NotBlank String keycloakClientId,
        MetadataStatus status,
        @Valid @NotNull CreateConnectorDto connector) {
    public MetadataStatus resolvedStatus() {
        return status == null ? MetadataStatus.ACTIVE : status;
    }
}
