package at.pegelhub.access.api;

import at.pegelhub.access.application.AccessGrantService;
import at.pegelhub.access.domain.AccessGrantId;
import at.pegelhub.connector.domain.ConnectorId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@RestController
@RequestMapping("/api/v1/access-grants")
@Tag(name = "Access Grants", description = "Manage connector access grants for stations and time series.")
@SecurityRequirement(name = "bearerAuth")
final class HttpAccessGrantController {

    private final AccessGrantService accessGrants;

    HttpAccessGrantController(AccessGrantService accessGrants) {
        this.accessGrants = requireNonNull(accessGrants);
    }

    @Operation(
            summary = "Creates an access grant",
            description = "Creates a connector permission for a station or time series. Requires METADATA_WRITE or SYSTEM_ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Returns the created access grant.",
                    content = @Content(schema = @Schema(implementation = AccessGrantResponse.class))),
            @ApiResponse(responseCode = "400", description = "The access grant payload is invalid.", content = @Content),
            @ApiResponse(responseCode = "404", description = "A referenced connector or resource was not found.", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AccessGrantResponse create(@Valid @RequestBody CreateAccessGrantRequest request) {
        return AccessGrantMapper.toResponse(accessGrants.create(AccessGrantMapper.toCommand(request)));
    }

    @Operation(
            summary = "Gets an access grant by ID",
            description = "Returns one access grant. Requires METADATA_READ, METADATA_WRITE, or SYSTEM_ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Returns the access grant.",
                    content = @Content(schema = @Schema(implementation = AccessGrantResponse.class))),
            @ApiResponse(responseCode = "400", description = "The access grant UUID is invalid.", content = @Content),
            @ApiResponse(responseCode = "404", description = "The access grant was not found.", content = @Content)
    })
    @GetMapping("/{id}")
    AccessGrantResponse get(@Parameter(description = "Access grant identifier.", required = true) @PathVariable UUID id) {
        return AccessGrantMapper.toResponse(accessGrants.get(new AccessGrantId(id)));
    }

    @Operation(
            summary = "Lists access grants",
            description = "Returns all access grants, optionally filtered by connectorId. Requires METADATA_READ, METADATA_WRITE, or SYSTEM_ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Returns access grants.",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AccessGrantResponse.class)))),
            @ApiResponse(responseCode = "400", description = "The connectorId query parameter is invalid.", content = @Content)
    })
    @GetMapping
    List<AccessGrantResponse> list(
            @Parameter(description = "Optional connector identifier to filter grants.", required = false)
            @RequestParam(required = false) UUID connectorId) {
        var result = connectorId == null
                ? accessGrants.list()
                : accessGrants.listForConnector(new ConnectorId(connectorId));
        return result.stream()
                .map(AccessGrantMapper::toResponse)
                .toList();
    }
}
