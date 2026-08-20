package at.pegelhub.connector.api;

import at.pegelhub.connector.domain.ConnectorType;
import at.pegelhub.shared.metadata.MetadataStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateConnectorRequest(@NotBlank String name, @NotNull ConnectorType type, @NotNull MetadataStatus status) {
}
