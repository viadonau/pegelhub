package at.pegelhub.connector.api;

import at.pegelhub.contact.api.CreateContactDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO to create connector data.
 */
@Schema(description = "Connector metadata to create.")
public record CreateConnectorDto(
        @Schema(description = "Unique connector number. Required and limited to 50 characters.", maxLength = 50, example = "connector-001")
        @NotBlank
        @Size(max = 50)
        String connectorNumber,
        @Schema(description = "Legacy manufacturer contact metadata.", nullable = true)
        @Valid
        CreateContactDto manufacturer,
        @Schema(description = "Connector type description. Limited to 100 characters.", maxLength = 100, example = "PLC water level gateway", nullable = true)
        @Size(max = 100)
        String typeDescription,
        @Schema(description = "Connector software version. Limited to 20 characters.", maxLength = 20, example = "1.0.0", nullable = true)
        @Size(max = 20)
        String softwareVersion,
        @Schema(description = "Earliest supported data definition version. Limited to 20 characters.", maxLength = 20, example = "1.0.0", nullable = true)
        @Size(max = 20)
        String worksFromDataVersion,
        @Schema(description = "Data definition identifier. Limited to 20 characters for create requests.", maxLength = 20, example = "pegelhub-v1", nullable = true)
        @Size(max = 20)
        String dataDefinition,
        @Schema(description = "Legacy software manufacturer contact metadata.", nullable = true)
        @Valid
        CreateContactDto softwareManufacturer,
        @Schema(description = "Legacy technically responsible contact metadata.", nullable = true)
        @Valid
        CreateContactDto technicallyResponsible,
        @Schema(description = "Legacy operation company contact metadata.", nullable = true)
        @Valid
        CreateContactDto operationCompany,
        @Schema(description = "Optional connector notes. Limited to 255 characters.", maxLength = 255, example = "Installed for local dev.", nullable = true)
        @Size(max = 255)
        String notes) {
    public CreateConnectorDto {
        typeDescription = nullToEmpty(typeDescription);
        softwareVersion = nullToEmpty(softwareVersion);
        worksFromDataVersion = nullToEmpty(worksFromDataVersion);
        dataDefinition = nullToEmpty(dataDefinition);
        notes = nullToEmpty(notes);
        manufacturer = nullToEmptyContact(manufacturer);
        softwareManufacturer = nullToEmptyContact(softwareManufacturer);
        technicallyResponsible = nullToEmptyContact(technicallyResponsible);
        operationCompany = nullToEmptyContact(operationCompany);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static CreateContactDto nullToEmptyContact(CreateContactDto contact) {
        return contact == null ? emptyContact() : contact;
    }

    private static CreateContactDto emptyContact() {
        return new CreateContactDto(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
