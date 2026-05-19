package at.pegelhub.taker.api;

import at.pegelhub.shared.web.DomainToDtoConverter;
import at.pegelhub.shared.web.DtoToDomainConverter;
import at.pegelhub.shared.web.*;

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

    private final TakerService takerService;

    public HttpTakerController(TakerService takerService) {
        this.takerService = requireNonNull(takerService);
    }

    @Operation(summary = "Saves a Taker to the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns the saved Taker")
    })
    @PostMapping
    public TakerDto saveTaker(@RequestBody CreateTakerDto taker) {
        return DomainToDtoConverter.convert(takerService.saveTaker(DtoToDomainConverter.convert(taker)));
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
}
