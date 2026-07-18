package at.pegelhub.connector.api;

import at.pegelhub.connector.domain.ConnectorStatus;
import at.pegelhub.contact.api.ContactDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

import static at.pegelhub.shared.validation.Validations.requireNotEmpty;
import static at.pegelhub.shared.validation.Validations.requireSEThan;
import static java.util.Objects.requireNonNull;

/**
 * DTO for connector data.
 */
@Schema(description = "Connector metadata.")
public record ConnectorDto(
        @Schema(description = "Connector identifier.", format = "uuid", example = "0d9a3c87-b41a-4663-af0a-f6ec5e6a91cf")
        UUID id,
        @Schema(description = "Unique connector number. Limited to 50 characters.", maxLength = 50, example = "connector-001")
        String connectorNumber,
        @Schema(description = "Legacy manufacturer contact metadata.", nullable = true)
        ContactDto manufacturer,
        @Schema(description = "Connector type description. Limited to 100 characters.", maxLength = 100, example = "PLC water level gateway", nullable = true)
        String typeDescription,
        @Schema(description = "Connector software version. Limited to 20 characters.", maxLength = 20, example = "1.0.0", nullable = true)
        String softwareVersion,
        @Schema(description = "Earliest supported data definition version. Limited to 20 characters.", maxLength = 20, example = "1.0.0", nullable = true)
        String worksFromDataVersion,
        @Schema(description = "Data definition identifier. Limited to 255 characters in responses.", maxLength = 255, example = "pegelhub-v1", nullable = true)
        String dataDefinition,
        @Schema(description = "Legacy software manufacturer contact metadata.", nullable = true)
        ContactDto softwareManufacturer,
        @Schema(description = "Legacy technically responsible contact metadata.", nullable = true)
        ContactDto technicallyResponsible,
        @Schema(description = "Legacy operation company contact metadata.", nullable = true)
        ContactDto operationCompany,
        @Schema(description = "Optional connector notes. Limited to 255 characters.", maxLength = 255, example = "Installed for local dev.", nullable = true)
        String notes,
        @Schema(description = "Keycloak client id bound to this connector, if registered.", example = "local-connector-example", nullable = true)
        String keycloakClientId,
        @Schema(description = "Connector lifecycle status.", example = "ACTIVE")
        ConnectorStatus status) {
    public ConnectorDto(UUID id, String connectorNumber, ContactDto manufacturer, String typeDescription,
                        String softwareVersion, String worksFromDataVersion, String dataDefinition,
                        ContactDto softwareManufacturer, ContactDto technicallyResponsible,
                        ContactDto operationCompany, String notes) {
        this(id, connectorNumber, manufacturer, typeDescription, softwareVersion, worksFromDataVersion, dataDefinition,
                softwareManufacturer, technicallyResponsible, operationCompany, notes, null, ConnectorStatus.ACTIVE);
    }

    public ConnectorDto {
        requireNonNull(id);
        requireNonNull(status);
        requireSEThan(requireNotEmpty(connectorNumber), 50);
        requireSEThan(typeDescription, 100);
        requireSEThan(softwareVersion, 20);
        requireSEThan(worksFromDataVersion, 20);
        requireSEThan(dataDefinition, 255);
        requireSEThan(notes, 255);
    }
}
