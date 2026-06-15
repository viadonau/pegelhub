package at.pegelhub.connector.api;

import at.pegelhub.connector.application.ConnectorService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static java.util.Objects.requireNonNull;

@RestController
@RequestMapping("/api/v1/admin/connectors")
public final class HttpAdminConnectorController {

    private final ConnectorService connectorService;

    HttpAdminConnectorController(ConnectorService connectorService) {
        this.connectorService = requireNonNull(connectorService);
    }

    @Operation(summary = "Registers a connector identity binding")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConnectorDto register(@Valid @RequestBody RegisterConnectorRequest request) {
        return ConnectorMapper.toResponse(connectorService.register(
                request.keycloakClientId(),
                request.resolvedStatus(),
                ConnectorMapper.toCommand(request.connector())));
    }
}
