package at.pegelhub.connector.api;

import at.pegelhub.connector.application.ConnectorService;
import at.pegelhub.shared.web.DomainToDtoConverter;
import at.pegelhub.shared.web.DtoToDomainConverter;
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
public class HttpAdminConnectorController {

    private final ConnectorService connectorService;

    public HttpAdminConnectorController(ConnectorService connectorService) {
        this.connectorService = requireNonNull(connectorService);
    }

    @Operation(summary = "Registers a connector identity binding")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConnectorDto registerConnector(@Valid @RequestBody RegisterConnectorRequest request) {
        return DomainToDtoConverter.convert(connectorService.registerConnector(
                request.keycloakClientId(),
                request.resolvedStatus(),
                DtoToDomainConverter.convert(request.connector())));
    }
}
