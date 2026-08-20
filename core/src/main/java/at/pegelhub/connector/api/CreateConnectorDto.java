package at.pegelhub.connector.api;

import at.pegelhub.connector.domain.ConnectorType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Connector metadata")
public record CreateConnectorDto(
        @NotBlank String name,
        @NotNull ConnectorType type) {
}
