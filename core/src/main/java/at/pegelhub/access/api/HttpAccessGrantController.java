package at.pegelhub.access.api;

import at.pegelhub.access.application.AccessGrantService;
import at.pegelhub.access.domain.AccessGrantId;
import at.pegelhub.connector.domain.ConnectorId;
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
final class HttpAccessGrantController {

    private final AccessGrantService accessGrants;

    HttpAccessGrantController(AccessGrantService accessGrants) {
        this.accessGrants = requireNonNull(accessGrants);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AccessGrantResponse create(@Valid @RequestBody CreateAccessGrantRequest request) {
        return AccessGrantMapper.toResponse(accessGrants.create(AccessGrantMapper.toCommand(request)));
    }

    @GetMapping("/{id}")
    AccessGrantResponse get(@PathVariable UUID id) {
        return AccessGrantMapper.toResponse(accessGrants.get(new AccessGrantId(id)));
    }

    @GetMapping
    List<AccessGrantResponse> list(@RequestParam(required = false) UUID connectorId) {
        var result = connectorId == null
                ? accessGrants.list()
                : accessGrants.listForConnector(new ConnectorId(connectorId));
        return result.stream()
                .map(AccessGrantMapper::toResponse)
                .toList();
    }
}
