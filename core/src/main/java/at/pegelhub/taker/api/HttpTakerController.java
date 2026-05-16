package at.pegelhub.taker.api;

import at.pegelhub.shared.web.DomainToDtoConverter;
import at.pegelhub.shared.web.DtoToDomainConverter;
import at.pegelhub.shared.web.*;

import at.pegelhub.auth.application.AuthTokenIdHolder;
import at.pegelhub.auth.application.AuthorizationService;
import at.pegelhub.taker.application.TakerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * REST controller for takers.
 */
@RestController
@RequestMapping("/api/v1/taker")
public class HttpTakerController {

    private final AuthorizationService authorizationService;
    private final TakerService takerService;

    public HttpTakerController(
            AuthorizationService authorizationService,
            TakerService takerService) {
        this.authorizationService = requireNonNull(authorizationService);
        this.takerService = requireNonNull(takerService);
    }

    @Operation(summary = "Saves a Taker to the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns the saved Taker")
    })
    @PostMapping
    public TakerDto saveTaker(
            @RequestParam(name = "apiKey", defaultValue = "") String apiKey,
            @RequestBody CreateTakerDto taker) {
        return runAsAuthorized(apiKey, () ->
                DomainToDtoConverter.convert(takerService.saveTaker(DtoToDomainConverter.convert(taker))));
    }

    @Operation(summary = "Gets a Taker by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns the Taker")
    })
    @GetMapping("/{uuid}")
    public TakerDto getTakerById(@PathVariable UUID uuid) {
        return DomainToDtoConverter.convert(takerService.getTakerById(uuid));
    }

    @Operation(summary = "Gets all Takers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns all Takers")
    })
    @GetMapping
    public List<TakerDto> getAllTakers() {
        return DomainToDtoConverter.convert(takerService.getAllTakers());
    }

    @Operation(summary = "Deletes a Taker by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200")
    })
    @DeleteMapping("/{uuid}")
    public void deleteTaker(@PathVariable UUID uuid) {
        takerService.deleteTaker(uuid);
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
