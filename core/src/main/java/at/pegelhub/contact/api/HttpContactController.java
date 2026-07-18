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
@Tag(name = "Legacy Contacts", description = "Legacy contact metadata endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class HttpContactController {


    private final ContactService contactService;

    public HttpContactController(ContactService contactService) {
        this.contactService = requireNonNull(contactService);
    }

    @Operation(
            summary = "Creates a legacy contact",
            description = "Creates a legacy contact metadata record. Requires METADATA_WRITE or SYSTEM_ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Returns the saved contact.",
                    content = @Content(schema = @Schema(implementation = ContactDto.class))),
            @ApiResponse(responseCode = "400", description = "The contact payload is invalid.", content = @Content)
    })
    @PostMapping
    public ContactDto saveContact(@RequestBody CreateContactDto contact) {
        return DomainToDtoConverter.convert(contactService.createContact(DtoToDomainConverter.convert(contact)));
    }

    @Operation(
            summary = "Gets a legacy contact by ID",
            description = "Returns a legacy contact metadata record. Requires METADATA_READ, METADATA_WRITE, or SYSTEM_ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Returns the contact.",
                    content = @Content(schema = @Schema(implementation = ContactDto.class))),
            @ApiResponse(responseCode = "400", description = "The contact UUID is invalid.", content = @Content),
            @ApiResponse(responseCode = "404", description = "The contact was not found.", content = @Content)
    })
    @GetMapping("/{uuid}")
    public ContactDto getContactById(@Parameter(description = "Contact identifier.", required = true) @PathVariable UUID uuid) {
        return DomainToDtoConverter.convert(contactService.getContactById(uuid));
    }

    @Operation(
            summary = "Lists legacy contacts",
            description = "Returns all legacy contact metadata records. Requires METADATA_READ, METADATA_WRITE, or SYSTEM_ADMIN.")
    @ApiResponse(
            responseCode = "200",
            description = "Returns all contacts.",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ContactDto.class))))
    @GetMapping
    public List<ContactDto> getAllContacts() {
        return DomainToDtoConverter.convert(contactService.getAllContacts());
    }

    @Operation(
            summary = "Deletes a legacy contact by ID",
            description = "Deletes a legacy contact metadata record. Requires METADATA_WRITE or SYSTEM_ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The contact was deleted."),
            @ApiResponse(responseCode = "400", description = "The contact UUID is invalid.", content = @Content),
            @ApiResponse(responseCode = "404", description = "The contact was not found.", content = @Content)
    })
    @DeleteMapping("/{uuid}")
    public void deleteContact(@Parameter(description = "Contact identifier.", required = true) @PathVariable UUID uuid) {
        contactService.deleteContact(uuid);
    }
}
