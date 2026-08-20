package at.pegelhub.connector.api;

import at.pegelhub.connector.domain.ConnectorType;
import at.pegelhub.shared.metadata.MetadataStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Operational connector client metadata")
public record ConnectorDto(
        UUID id,
        String name,
        ConnectorType type,
        String keycloakClientId,
        MetadataStatus status) {
}
