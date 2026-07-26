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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@RestController
@RequestMapping("/api/v1/connectors")
@Tag(name = "Connectors", description = "openapi.connector.http-connector-controller.manage-connector-metadata")
@SecurityRequirement(name = "bearerAuth")
public final class HttpConnectorController {

    private final ConnectorService connectorService;

    HttpConnectorController(ConnectorService connectorService) {
        this.connectorService = requireNonNull(connectorService);
    }

    @Operation(
            summary = "openapi.connector.http-connector-controller.creates-a-connector",
            description = "openapi.connector.http-connector-controller.creates-connector-metadata-requires-metadata-write-or")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "openapi.connector.http-connector-controller.returns-the-created-connector",
                    content = @Content(schema = @Schema(implementation = ConnectorDto.class))),
            @ApiResponse(responseCode = "400", description = "openapi.connector.http-connector-controller.the-connector-payload-is-invalid", content = @Content)
    })
    @PostMapping
    public ConnectorDto create(@RequestBody CreateConnectorDto dto) {
        return ConnectorMapper.toResponse(connectorService.create(ConnectorMapper.toCommand(dto)));
    }

    @Operation(
            summary = "openapi.connector.http-connector-controller.gets-a-connector-by-id",
            description = "openapi.connector.http-connector-controller.returns-connector-metadata-for-a-uuid-requires")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "openapi.connector.http-connector-controller.returns-the-connector",
                    content = @Content(schema = @Schema(implementation = ConnectorDto.class))),
            @ApiResponse(responseCode = "400", description = "openapi.connector.http-connector-controller.the-connector-uuid-is-invalid", content = @Content),
            @ApiResponse(responseCode = "404", description = "openapi.connector.http-connector-controller.the-connector-was-not-found", content = @Content)
    })
    @GetMapping("/{uuid}")
    public ConnectorDto get(@Parameter(description = "openapi.connector.connector-dto.connector-identifier", required = true) @PathVariable UUID uuid) {
        return ConnectorMapper.toResponse(connectorService.get(new ConnectorId(uuid)));
    }

    @Operation(
            summary = "openapi.connector.http-connector-controller.lists-all-connectors",
            description = "openapi.connector.http-connector-controller.returns-all-connector-metadata-records-requires-metadata")
    @ApiResponse(
            responseCode = "200",
            description = "openapi.connector.http-connector-controller.returns-all-connectors",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ConnectorDto.class))))
    @GetMapping
    public List<ConnectorDto> list() {
        return connectorService.list().stream().map(ConnectorMapper::toResponse).toList();
    }

    @Operation(
            summary = "openapi.connector.http-connector-controller.deletes-a-connector-by-id",
            description = "openapi.connector.http-connector-controller.deletes-connector-metadata-for-a-uuid-requires")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "openapi.connector.http-connector-controller.the-connector-was-deleted"),
            @ApiResponse(responseCode = "400", description = "openapi.connector.http-connector-controller.the-connector-uuid-is-invalid", content = @Content),
            @ApiResponse(responseCode = "404", description = "openapi.connector.http-connector-controller.the-connector-was-not-found", content = @Content)
    })
    @DeleteMapping("/{uuid}")
    public void delete(@Parameter(description = "openapi.connector.connector-dto.connector-identifier", required = true) @PathVariable UUID uuid) {
        connectorService.delete(new ConnectorId(uuid));
    }
}
