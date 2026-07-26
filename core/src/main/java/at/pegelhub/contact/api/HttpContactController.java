package at.pegelhub.contact.api;

import at.pegelhub.shared.web.DomainToDtoConverter;
import at.pegelhub.shared.web.DtoToDomainConverter;
import at.pegelhub.shared.web.*;

import at.pegelhub.contact.application.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * REST controller for contacts.
 */
@RestController
@RequestMapping("/api/v1/contact")
@Tag(name = "Legacy Contacts", description = "openapi.contact.http-contact-controller.legacy-contact-metadata-endpoints")
@SecurityRequirement(name = "bearerAuth")
public class HttpContactController {


    private final ContactService contactService;

    public HttpContactController(ContactService contactService) {
        this.contactService = requireNonNull(contactService);
    }

    @Operation(
            summary = "openapi.contact.http-contact-controller.creates-a-legacy-contact",
            description = "openapi.contact.http-contact-controller.creates-a-legacy-contact-metadata-record-requires")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "openapi.contact.http-contact-controller.returns-the-saved-contact",
                    content = @Content(schema = @Schema(implementation = ContactDto.class))),
            @ApiResponse(responseCode = "400", description = "openapi.contact.http-contact-controller.the-contact-payload-is-invalid", content = @Content)
    })
    @PostMapping
    public ContactDto saveContact(@RequestBody CreateContactDto contact) {
        return DomainToDtoConverter.convert(contactService.createContact(DtoToDomainConverter.convert(contact)));
    }

    @Operation(
            summary = "openapi.contact.http-contact-controller.gets-a-legacy-contact-by-id",
            description = "openapi.contact.http-contact-controller.returns-a-legacy-contact-metadata-record-requires")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "openapi.contact.http-contact-controller.returns-the-contact",
                    content = @Content(schema = @Schema(implementation = ContactDto.class))),
            @ApiResponse(responseCode = "400", description = "openapi.contact.http-contact-controller.the-contact-uuid-is-invalid", content = @Content),
            @ApiResponse(responseCode = "404", description = "openapi.contact.http-contact-controller.the-contact-was-not-found", content = @Content)
    })
    @GetMapping("/{uuid}")
    public ContactDto getContactById(@Parameter(description = "openapi.contact.contact-dto.contact-identifier", required = true) @PathVariable UUID uuid) {
        return DomainToDtoConverter.convert(contactService.getContactById(uuid));
    }

    @Operation(
            summary = "openapi.contact.http-contact-controller.lists-legacy-contacts",
            description = "openapi.contact.http-contact-controller.returns-all-legacy-contact-metadata-records-requires")
    @ApiResponse(
            responseCode = "200",
            description = "openapi.contact.http-contact-controller.returns-all-contacts",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ContactDto.class))))
    @GetMapping
    public List<ContactDto> getAllContacts() {
        return DomainToDtoConverter.convert(contactService.getAllContacts());
    }

    @Operation(
            summary = "openapi.contact.http-contact-controller.deletes-a-legacy-contact-by-id",
            description = "openapi.contact.http-contact-controller.deletes-a-legacy-contact-metadata-record-requires")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "openapi.contact.http-contact-controller.the-contact-was-deleted"),
            @ApiResponse(responseCode = "400", description = "openapi.contact.http-contact-controller.the-contact-uuid-is-invalid", content = @Content),
            @ApiResponse(responseCode = "404", description = "openapi.contact.http-contact-controller.the-contact-was-not-found", content = @Content)
    })
    @DeleteMapping("/{uuid}")
    public void deleteContact(@Parameter(description = "openapi.contact.contact-dto.contact-identifier", required = true) @PathVariable UUID uuid) {
        contactService.deleteContact(uuid);
    }
}
