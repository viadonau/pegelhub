package at.pegelhub.contact.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * DTO to create contact data.
 */
@Schema(description = "openapi.contact.contact-dto.legacy-contact-metadata")
public record ContactDto(
        @Schema(description = "openapi.contact.contact-dto.contact-identifier", format = "uuid", example = "0d0b8c82-3716-49d4-b2f2-9b1667bbbc1e")
        UUID id,
        @Schema(description = "openapi.contact.contact-dto.organization-name", example = "Hydro Systems GmbH", nullable = true)
        String organization,
        @Schema(description = "openapi.contact.contact-dto.contact-person-name", example = "Alex Meyer", nullable = true)
        String contactPerson,
        @Schema(description = "openapi.contact.contact-dto.street-address", example = "River Street 1", nullable = true)
        String contactStreet,
        @Schema(description = "openapi.contact.contact-dto.postal-code", example = "1020", nullable = true)
        String contactPlz,
        @Schema(description = "openapi.contact.contact-dto.city-or-location", example = "Vienna", nullable = true)
        String location,
        @Schema(description = "openapi.contact.contact-dto.country", example = "AT", nullable = true)
        String contactCountry,
        @Schema(description = "openapi.contact.contact-dto.primary-emergency-phone-number", example = "+43 1 5550100", nullable = true)
        String emergencyNumber,
        @Schema(description = "openapi.contact.contact-dto.secondary-emergency-phone-number", example = "+43 1 5550101", nullable = true)
        String emergencyNumberTwo,
        @Schema(description = "openapi.contact.contact-dto.emergency-email-address", example = "emergency@example.com", nullable = true)
        String emergencyMail,
        @Schema(description = "openapi.contact.contact-dto.primary-service-phone-number", example = "+43 1 5550200", nullable = true)
        String serviceNumber,
        @Schema(description = "openapi.contact.contact-dto.secondary-service-phone-number", example = "+43 1 5550201", nullable = true)
        String serviceNumberTwo,
        @Schema(description = "openapi.contact.contact-dto.service-email-address", example = "service@example.com", nullable = true)
        String serviceMail,
        @Schema(description = "openapi.contact.contact-dto.primary-administration-phone-number", example = "+43 1 5550300", nullable = true)
        String administrationPhoneNumber,
        @Schema(description = "openapi.contact.contact-dto.secondary-administration-phone-number", example = "+43 1 5550301", nullable = true)
        String administrationPhoneNumberTwo,
        @Schema(description = "openapi.contact.contact-dto.administration-email-address", example = "admin@example.com", nullable = true)
        String administrationMail,
        @Schema(description = "openapi.contact.contact-dto.free-text-contact-notes", example = "Available during business hours.", nullable = true)
        String contactNodes) {
    public ContactDto {
        requireNonNull(id);
    }
}
