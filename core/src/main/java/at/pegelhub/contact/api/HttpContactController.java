package at.pegelhub.contact.api;

import at.pegelhub.shared.web.DomainToDtoConverter;
import at.pegelhub.shared.web.DtoToDomainConverter;
import at.pegelhub.shared.web.*;

import at.pegelhub.contact.application.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * REST controller for contacts.
 */
@RestController
@RequestMapping("/api/v1/contact")
public class HttpContactController {


    private final ContactService contactService;

    public HttpContactController(ContactService contactService) {
        this.contactService = requireNonNull(contactService);
    }

    @Operation(summary = "Saves a Contact to the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns the saved Contact")
    })
    @PostMapping
    public ContactDto saveContact(@RequestBody CreateContactDto contact) {
        return DomainToDtoConverter.convert(contactService.createContact(DtoToDomainConverter.convert(contact)));
    }

    @Operation(summary = "Gets a Contact by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns the Contact")
    })
    @GetMapping("/{uuid}")
    public ContactDto getContactById(@PathVariable UUID uuid) {
        return DomainToDtoConverter.convert(contactService.getContactById(uuid));
    }

    @Operation(summary = "Gets all Contacts")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns all Contacts")
    })
    @GetMapping
    public List<ContactDto> getAllContacts() {
        return DomainToDtoConverter.convert(contactService.getAllContacts());
    }

    @Operation(summary = "Deletes a Contact by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200")
    })
    @DeleteMapping("/{uuid}")
    public void deleteContact(@PathVariable UUID uuid) {
        contactService.deleteContact(uuid);
    }
}
