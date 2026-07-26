package at.pegelhub.connector.api;

import at.pegelhub.contact.api.CreateContactDto;
import io.swagger.v3.oas.annotations.media.Schema;

import static at.pegelhub.shared.validation.Validations.requireNotEmpty;
import static at.pegelhub.shared.validation.Validations.requireSEThan;

/**
 * DTO to create connector data.
 */
@Schema(description = "openapi.connector.create-connector-dto.connector-metadata-to-create")
public record CreateConnectorDto(
        @Schema(description = "openapi.connector.create-connector-dto.unique-connector-number-required-and-limited-to", maxLength = 50,
                example = "connector-001")
        String connectorNumber,
        @Schema(description = "openapi.connector.connector-dto.legacy-manufacturer-contact-metadata")
        CreateContactDto manufacturer,
        @Schema(description = "openapi.connector.create-connector-dto.connector-type-description-required-and-limited-to", maxLength = 100,
                example = "PLC water level gateway")
        String typeDescription,
        @Schema(description = "openapi.connector.create-connector-dto.connector-software-version-required-and-limited-to", maxLength = 20,
                example = "1.0.0")
        String softwareVersion,
        @Schema(description = "openapi.connector.create-connector-dto.earliest-supported-data-definition-version-required-and",
                maxLength = 20, example = "1.0.0")
        String worksFromDataVersion,
        @Schema(description = "openapi.connector.create-connector-dto.data-definition-identifier-required-and-limited-to",
                maxLength = 20, example = "pegelhub-v1")
        String dataDefinition,
        @Schema(description = "openapi.connector.connector-dto.legacy-software-manufacturer-contact-metadata")
        CreateContactDto softwareManufacturer,
        @Schema(description = "openapi.connector.connector-dto.legacy-technically-responsible-contact-metadata")
        CreateContactDto technicallyResponsible,
        @Schema(description = "openapi.connector.connector-dto.legacy-operation-company-contact-metadata")
        CreateContactDto operationCompany,
        @Schema(description = "openapi.connector.create-connector-dto.connector-notes-required-may-be-empty-and", maxLength = 255,
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
