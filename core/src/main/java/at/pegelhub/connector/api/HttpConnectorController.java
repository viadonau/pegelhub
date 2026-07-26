package at.pegelhub.connector.api;

import at.pegelhub.connector.application.ConnectorService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@RestController
@RequestMapping("/api/v1/connectors")
@Tag(name = "Connectors", description = "Manage connector metadata.")
@SecurityRequirement(name = "bearerAuth")
public final class HttpConnectorController {

    private final ConnectorService connectorService;

    HttpConnectorController(ConnectorService connectorService) {
        this.connectorService = requireNonNull(connectorService);
    }

    @Operation(
            summary = "Creates a connector",
            description = "Creates connector metadata. Requires METADATA_WRITE or SYSTEM_ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Returns the created connector.",
                    content = @Content(schema = @Schema(implementation = ConnectorDto.class))),
            @ApiResponse(responseCode = "400", description = "The connector payload is invalid.", content = @Content)
    })
    @PostMapping
    public ConnectorDto create(@Valid @RequestBody CreateConnectorDto dto) {
        return ConnectorMapper.toResponse(connectorService.create(ConnectorMapper.toCommand(dto)));
    }

    @Operation(
            summary = "Gets a connector by ID",
            description = "Returns connector metadata for a UUID. Requires METADATA_READ, METADATA_WRITE, or SYSTEM_ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Returns the connector.",
                    content = @Content(schema = @Schema(implementation = ConnectorDto.class))),
            @ApiResponse(responseCode = "400", description = "The connector UUID is invalid.", content = @Content),
            @ApiResponse(responseCode = "404", description = "The connector was not found.", content = @Content)
    })
    @GetMapping("/{uuid}")
    public ConnectorDto get(@Parameter(description = "Connector identifier.", required = true) @PathVariable UUID uuid) {
        return ConnectorMapper.toResponse(connectorService.get(new ConnectorId(uuid)));
    }

    @Operation(
            summary = "Lists all connectors",
            description = "Returns all connector metadata records. Requires METADATA_READ, METADATA_WRITE, or SYSTEM_ADMIN.")
    @ApiResponse(
            responseCode = "200",
            description = "Returns all connectors.",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ConnectorDto.class))))
    @GetMapping
    public List<ConnectorDto> list() {
        return connectorService.list().stream().map(ConnectorMapper::toResponse).toList();
    }

    @Operation(
            summary = "Deletes a connector by ID",
            description = "Deletes connector metadata for a UUID. Requires METADATA_WRITE or SYSTEM_ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The connector was deleted."),
            @ApiResponse(responseCode = "400", description = "The connector UUID is invalid.", content = @Content),
            @ApiResponse(responseCode = "404", description = "The connector was not found.", content = @Content)
    })
    @DeleteMapping("/{uuid}")
    public void delete(@Parameter(description = "Connector identifier.", required = true) @PathVariable UUID uuid) {
        connectorService.delete(new ConnectorId(uuid));
    }
}
