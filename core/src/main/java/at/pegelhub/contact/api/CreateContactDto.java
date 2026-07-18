package at.pegelhub.contact.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * DTO to create contact data.
 */
@Schema(description = "Legacy contact metadata to create.")
public record CreateContactDto(
        @Schema(description = "Organization name.", maxLength = 150, example = "Hydro Systems GmbH", nullable = true)
        @Size(max = 150)
        String organization,
        @Schema(description = "Contact person name.", maxLength = 150, example = "Alex Meyer", nullable = true)
        @Size(max = 150)
        String contactPerson,
        @Schema(description = "Street address.", maxLength = 150, example = "River Street 1", nullable = true)
        @Size(max = 150)
        String contactStreet,
        @Schema(description = "Postal code.", maxLength = 50, example = "1020", nullable = true)
        @Size(max = 50)
        String contactPlz,
        @Schema(description = "City or location.", maxLength = 50, example = "Vienna", nullable = true)
        @Size(max = 50)
        String location,
        @Schema(description = "Country.", maxLength = 50, example = "AT", nullable = true)
        @Size(max = 50)
        String contactCountry,
        @Schema(description = "Primary emergency phone number.", maxLength = 50, example = "+43 1 5550100", nullable = true)
        @Size(max = 50)
        String emergencyNumber,
        @Schema(description = "Secondary emergency phone number.", maxLength = 50, example = "+43 1 5550101", nullable = true)
        @Size(max = 50)
        String emergencyNumberTwo,
        @Schema(description = "Emergency email address.", maxLength = 50, example = "emergency@example.com", nullable = true)
        @Size(max = 50)
        String emergencyMail,
        @Schema(description = "Primary service phone number.", maxLength = 50, example = "+43 1 5550200", nullable = true)
        @Size(max = 50)
        String serviceNumber,
        @Schema(description = "Secondary service phone number.", maxLength = 50, example = "+43 1 5550201", nullable = true)
        @Size(max = 50)
        String serviceNumberTwo,
        @Schema(description = "Service email address.", maxLength = 50, example = "service@example.com", nullable = true)
        @Size(max = 50)
        String serviceMail,
        @Schema(description = "Primary administration phone number.", maxLength = 50, example = "+43 1 5550300", nullable = true)
        @Size(max = 50)
        String administrationPhoneNumber,
        @Schema(description = "Secondary administration phone number.", maxLength = 50, example = "+43 1 5550301", nullable = true)
        @Size(max = 50)
        String administrationPhoneNumberTwo,
        @Schema(description = "Administration email address.", maxLength = 50, example = "admin@example.com", nullable = true)
        @Size(max = 50)
        String administrationMail,
        @Schema(description = "Free-text contact notes.", maxLength = 255, example = "Available during business hours.", nullable = true)
        @Size(max = 255)
        String contactNodes) {
}
