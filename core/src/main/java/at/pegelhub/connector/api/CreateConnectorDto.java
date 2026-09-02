package at.pegelhub.connector.api;

import at.pegelhub.connector.domain.ConnectorType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "openapi.connector.create-connector-dto.connector-metadata-to-create")
public record CreateConnectorDto(
        @Schema(description = "openapi.connector.connector-dto.connector-name", example = "Danube FTP import")
        @NotBlank String name,
        @Schema(description = "openapi.connector.connector-dto.connector-type")
        @NotNull ConnectorType type) {
}
