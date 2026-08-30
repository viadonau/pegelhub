package at.pegelhub.connector.api;

import at.pegelhub.connector.domain.ConnectorType;
import at.pegelhub.shared.metadata.MetadataStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "openapi.connector.connector-dto.connector-metadata")
public record ConnectorDto(
        @Schema(description = "openapi.connector.connector-dto.connector-identifier", format = "uuid", example = "a93fdc3d-b71f-44ce-a826-fe1dc1f1f357")
        UUID id,
        @Schema(description = "openapi.connector.connector-dto.connector-name", example = "Danube FTP import")
        String name,
        @Schema(description = "openapi.connector.connector-dto.connector-type")
        ConnectorType type,
        @Schema(description = "openapi.connector.connector-dto.keycloak-client-id-bound-to-this-connector", example = "danube-ftp-import", nullable = true)
        String keycloakClientId,
        @Schema(description = "openapi.connector.connector-dto.connector-lifecycle-status")
        MetadataStatus status) {
}
