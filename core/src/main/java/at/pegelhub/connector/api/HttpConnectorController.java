package at.pegelhub.connector.api;

import at.pegelhub.connector.application.ConnectorService;
import at.pegelhub.connector.domain.ConnectorId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@RestController
@RequestMapping("/api/v1/connectors")
public final class HttpConnectorController {

    private final ConnectorService connectorService;

    HttpConnectorController(ConnectorService connectorService) {
        this.connectorService = requireNonNull(connectorService);
    }

    @Operation(summary = "Creates a connector")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns the created connector")
    })
    @PostMapping
    public ConnectorDto create(@RequestBody CreateConnectorDto dto) {
        return ConnectorMapper.toResponse(connectorService.create(ConnectorMapper.toCommand(dto)));
    }

    @Operation(summary = "Gets a connector by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns the connector")
    })
    @GetMapping("/{uuid}")
    public ConnectorDto get(@PathVariable UUID uuid) {
        return ConnectorMapper.toResponse(connectorService.get(new ConnectorId(uuid)));
    }

    @Operation(summary = "Lists all connectors")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns all connectors")
    })
    @GetMapping
    public List<ConnectorDto> list() {
        return connectorService.list().stream().map(ConnectorMapper::toResponse).toList();
    }

    @Operation(summary = "Deletes a connector by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200")
    })
    @DeleteMapping("/{uuid}")
    public void delete(@PathVariable UUID uuid) {
        connectorService.delete(new ConnectorId(uuid));
    }
}
