package at.pegelhub.contact.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * DTO to create contact data.
 */
@Schema(description = "Legacy contact metadata.")
public record ContactDto(
        @Schema(description = "Contact identifier.", format = "uuid", example = "0d0b8c82-3716-49d4-b2f2-9b1667bbbc1e")
        UUID id,
        @Schema(description = "Organization name.", example = "Hydro Systems GmbH", nullable = true)
        String organization,
        @Schema(description = "Contact person name.", example = "Alex Meyer", nullable = true)
        String contactPerson,
        @Schema(description = "Street address.", example = "River Street 1", nullable = true)
        String contactStreet,
        @Schema(description = "Postal code.", example = "1020", nullable = true)
        String contactPlz,
        @Schema(description = "City or location.", example = "Vienna", nullable = true)
        String location,
        @Schema(description = "Country.", example = "AT", nullable = true)
        String contactCountry,
        @Schema(description = "Primary emergency phone number.", example = "+43 1 5550100", nullable = true)
        String emergencyNumber,
        @Schema(description = "Secondary emergency phone number.", example = "+43 1 5550101", nullable = true)
        String emergencyNumberTwo,
        @Schema(description = "Emergency email address.", example = "emergency@example.com", nullable = true)
        String emergencyMail,
        @Schema(description = "Primary service phone number.", example = "+43 1 5550200", nullable = true)
        String serviceNumber,
        @Schema(description = "Secondary service phone number.", example = "+43 1 5550201", nullable = true)
        String serviceNumberTwo,
        @Schema(description = "Service email address.", example = "service@example.com", nullable = true)
        String serviceMail,
        @Schema(description = "Primary administration phone number.", example = "+43 1 5550300", nullable = true)
        String administrationPhoneNumber,
        @Schema(description = "Secondary administration phone number.", example = "+43 1 5550301", nullable = true)
        String administrationPhoneNumberTwo,
        @Schema(description = "Administration email address.", example = "admin@example.com", nullable = true)
        String administrationMail,
        @Schema(description = "Free-text contact notes.", example = "Available during business hours.", nullable = true)
        String contactNodes) {
    public ContactDto {
        requireNonNull(id);
    }
}
