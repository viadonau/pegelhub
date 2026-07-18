package at.pegelhub.connector.api;

import at.pegelhub.contact.api.CreateContactDto;
import io.swagger.v3.oas.annotations.media.Schema;

import static at.pegelhub.shared.validation.Validations.requireNotEmpty;
import static at.pegelhub.shared.validation.Validations.requireSEThan;

/**
 * DTO to create connector data.
 */
@Schema(description = "Connector metadata to create.")
public record CreateConnectorDto(
        @Schema(description = "Unique connector number. Required and limited to 50 characters.", maxLength = 50,
                example = "connector-001")
        String connectorNumber,
        @Schema(description = "Legacy manufacturer contact metadata.")
        CreateContactDto manufacturer,
        @Schema(description = "Connector type description. Required and limited to 100 characters.", maxLength = 100,
                example = "PLC water level gateway")
        String typeDescription,
        @Schema(description = "Connector software version. Required and limited to 20 characters.", maxLength = 20,
                example = "1.0.0")
        String softwareVersion,
        @Schema(description = "Earliest supported data definition version. Required and limited to 20 characters.",
                maxLength = 20, example = "1.0.0")
        String worksFromDataVersion,
        @Schema(description = "Data definition identifier. Required and limited to 20 characters for create requests.",
                maxLength = 20, example = "pegelhub-v1")
        String dataDefinition,
        @Schema(description = "Legacy software manufacturer contact metadata.")
        CreateContactDto softwareManufacturer,
        @Schema(description = "Legacy technically responsible contact metadata.")
        CreateContactDto technicallyResponsible,
        @Schema(description = "Legacy operation company contact metadata.")
        CreateContactDto operationCompany,
        @Schema(description = "Connector notes. Required, may be empty, and limited to 255 characters.", maxLength = 255,
                example = "Installed for local dev.")
        String notes) {
    public CreateConnectorDto {
        requireSEThan(requireNotEmpty(connectorNumber), 50);
        requireSEThan(typeDescription, 100);
        requireSEThan(softwareVersion, 20);
        requireSEThan(worksFromDataVersion, 20);
        requireSEThan(dataDefinition, 20);
        requireSEThan(notes, 255);
    }
}
