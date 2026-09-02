package at.pegelhub.connector.api;

import at.pegelhub.connector.domain.ConnectorType;
import at.pegelhub.shared.metadata.MetadataStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "openapi.connector.update-connector-request.request-to-replace-connector-metadata")
public record UpdateConnectorRequest(
        @Schema(description = "openapi.connector.connector-dto.connector-name", example = "Danube FTP import")
        @NotBlank String name,
        @Schema(description = "openapi.connector.connector-dto.connector-type")
        @NotNull ConnectorType type,
        @Schema(description = "openapi.connector.connector-dto.connector-lifecycle-status")
        @NotNull MetadataStatus status) {
}
