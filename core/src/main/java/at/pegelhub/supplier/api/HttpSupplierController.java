package at.pegelhub.supplier.api;

import at.pegelhub.shared.web.DomainToDtoConverter;
import at.pegelhub.shared.web.DtoToDomainConverter;
import at.pegelhub.shared.web.*;

import at.pegelhub.auth.application.AuthTokenIdHolder;
import at.pegelhub.auth.application.AuthorizationService;
import at.pegelhub.supplier.application.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * REST controller for suppliers.
 */
@RestController
@RequestMapping("/api/v1/supplier")
public class HttpSupplierController {

    private final AuthorizationService authorizationService;
    private final SupplierService supplierService;

    public HttpSupplierController(
            AuthorizationService authorizationService,
            SupplierService supplierService) {
        this.authorizationService = requireNonNull(authorizationService);
        this.supplierService = requireNonNull(supplierService);
    }

    @Operation(summary = "Saves a Supplier to the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns the saved Supplier")
    })
    @PostMapping
    public SupplierDto saveSupplier(
            @RequestParam(name = "apiKey", defaultValue = "") String apiKey,
            @RequestBody CreateSupplierDto supplier) {
        return runAsAuthorized(apiKey, () ->
                DomainToDtoConverter.convert(supplierService.saveSupplier(DtoToDomainConverter.convert(supplier))));
    }

    @GetMapping("/{uuid}")
    public SupplierDto getSupplierById(@PathVariable UUID uuid) {
        return DomainToDtoConverter.convert(supplierService.getSupplierById(uuid));
    }

    @Operation(summary = "Gets all Suppliers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns all Suppliers")
    })
    @GetMapping
    public List<SupplierDto> getAllSuppliers() {
        return DomainToDtoConverter.convert(supplierService.getAllSuppliers());
    }

    @Operation(summary = "Deletes a Supplier by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200")
    })
    @DeleteMapping("/{uuid}")
    public void deleteSupplier(@PathVariable UUID uuid) {
        supplierService.deleteSupplier(uuid);
    }

    @GetMapping("/connectorID/{uuid}")
    public UUID getConnectorID(@PathVariable UUID uuid) {
        return supplierService.getConnectorID(uuid);
    }

    @Operation(summary = "Gets a Supplier by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns the Supplier")
    })
    @PutMapping
    public SupplierDto updateSupplier(
            @RequestParam(name = "apiKey", defaultValue = "") String apiKey,
            @RequestBody CreateSupplierDto supplier) {
        return runAsAuthorized(apiKey, () ->
                DomainToDtoConverter.convert(supplierService.updateSupplier(DtoToDomainConverter.convert(supplier))));
    }

    private <T> T runAsAuthorized(String apiKey, AuthorizedSupplier<T> action) {
        AuthTokenIdHolder.set(authorizationService.authorize(apiKey));
        try {
            return action.run();
        } finally {
            AuthTokenIdHolder.clear();
        }
    }

    @FunctionalInterface
    private interface AuthorizedSupplier<T> {
        T run();
    }
}
