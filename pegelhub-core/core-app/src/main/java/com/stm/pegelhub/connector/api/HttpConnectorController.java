package com.stm.pegelhub.connector.api;

import com.stm.pegelhub.shared.web.*;

import com.stm.pegelhub.connector.api.ConnectorDto;
import com.stm.pegelhub.connector.api.CreateConnectorDto;
import com.stm.pegelhub.connector.application.ConnectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * REST controller for connectors.
 */
@RestController
@RequestMapping("/api/v1/connector")
public class HttpConnectorController {

    private final ConnectorService connectorService;

    public HttpConnectorController(ConnectorService connectorService) {
        this.connectorService = requireNonNull(connectorService);
    }

    @Operation(summary = "Saves a Connector to the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns the saved Connector")
    })
    @PostMapping
    public ConnectorDto saveConnector(@RequestBody CreateConnectorDto connector) {
        return DomainToDtoConverter.convert(connectorService.createConnector(DtoToDomainConverter.convert(connector)));
    }

    @Operation(summary = "Gets a Connector by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns the Connector")
    })
    @GetMapping("/{uuid}")
    public ConnectorDto getConnectorById(@PathVariable UUID uuid) {
        return DomainToDtoConverter.convert(connectorService.getConnectorById(uuid));
    }

    @Operation(summary = "Gets all Connectors")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns all Connectors")
    })
    @GetMapping
    public List<ConnectorDto> getAllConnectors() {
        return DomainToDtoConverter.convert(connectorService.getAllConnectors());
    }

    @Operation(summary = "Deletes a Connector by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200")
    })
    @DeleteMapping("/{uuid}")
    public void deleteConnector(@PathVariable UUID uuid) {
        connectorService.deleteConnector(uuid);
    }
}
