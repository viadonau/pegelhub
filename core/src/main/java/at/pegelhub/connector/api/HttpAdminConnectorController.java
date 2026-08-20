package at.pegelhub.connector.api;

import at.pegelhub.connector.application.ConnectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@RestController
@RequestMapping("/api/v1/admin/connectors")
@Tag(name = "Connector Admin", description = "openapi.connector.http-admin-connector-controller.administrative-connector-identity-binding-endpoints")
@SecurityRequirement(name = "bearerAuth")
public final class HttpAdminConnectorController {

    private final ConnectorService connectorService;

    HttpAdminConnectorController(ConnectorService connectorService) {
        this.connectorService = requireNonNull(connectorService);
    }

    @Operation(
            operationId = "registerConnectorIdentity",
            summary = "openapi.connector.http-admin-connector-controller.registers-a-connector-identity-binding",
            description = "openapi.connector.http-admin-connector-controller.creates-connector-metadata-and-binds-it-to")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "openapi.connector.http-admin-connector-controller.returns-the-registered-connector",
                    content = @Content(schema = @Schema(implementation = ConnectorDto.class))),
            @ApiResponse(responseCode = "400", description = "openapi.connector.http-admin-connector-controller.the-registration-payload-is-invalid", content = @Content),
            @ApiResponse(responseCode = "409", description = "openapi.connector.http-admin-connector-controller.connector-client-already-registered", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConnectorDto register(@Valid @RequestBody RegisterConnectorRequest request) {
        return ConnectorMapper.toResponse(connectorService.register(
                request.keycloakClientId(),
                request.resolvedStatus(),
                ConnectorMapper.toCommand(request.connector())));
    }

    @Operation(
            operationId = "updateConnector",
            summary = "openapi.connector.http-admin-connector-controller.updates-a-connector",
            description = "openapi.connector.http-admin-connector-controller.replaces-mutable-connector-metadata")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "openapi.connector.http-admin-connector-controller.returns-the-updated-connector",
                    content = @Content(schema = @Schema(implementation = ConnectorDto.class))),
            @ApiResponse(responseCode = "400", description = "openapi.connector.http-admin-connector-controller.the-update-payload-is-invalid", content = @Content),
            @ApiResponse(responseCode = "404", description = "openapi.connector.http-admin-connector-controller.the-connector-was-not-found", content = @Content)
    })
    @PutMapping("/{id}")
    public ConnectorDto update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateConnectorRequest request) {
        return ConnectorMapper.toResponse(connectorService.update(
                new at.pegelhub.connector.domain.ConnectorId(id), ConnectorMapper.toCommand(request)));
    }
}
