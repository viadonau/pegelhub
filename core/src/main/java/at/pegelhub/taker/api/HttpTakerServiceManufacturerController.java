package at.pegelhub.taker.api;

import at.pegelhub.shared.web.DomainToDtoConverter;
import at.pegelhub.shared.web.DtoToDomainConverter;
import at.pegelhub.shared.web.*;

import at.pegelhub.taker.application.TakerServiceManufacturerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * REST controller for taker service manufacturers.
 */
@RestController
@RequestMapping("/api/v1/takerServiceManufacturer")
public class HttpTakerServiceManufacturerController {

    private final TakerServiceManufacturerService takerServiceManufacturerService;

    public HttpTakerServiceManufacturerController(TakerServiceManufacturerService takerServiceManufacturerService) {
        this.takerServiceManufacturerService = requireNonNull(takerServiceManufacturerService);
    }

    @Operation(summary = "Saves a TakerServiceManufacturer to the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns the saved TakerServiceManufacturer")
    })
    @PostMapping
    public TakerServiceManufacturerDto saveTakerServiceManufacturer(
            @RequestBody CreateTakerServiceManufacturerDto takerServiceManufacturer) {
        return DomainToDtoConverter.convert(takerServiceManufacturerService.createTakerServiceManufacturer(DtoToDomainConverter.convert(takerServiceManufacturer)));
    }

    @Operation(summary = "Gets a TakerServiceManufacturer by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns the TakerServiceManufacturer")
    })
    @GetMapping("/{uuid}")
    public TakerServiceManufacturerDto getTakerServiceManufacturerById(@PathVariable UUID uuid) {
        return DomainToDtoConverter.convert(takerServiceManufacturerService.getTakerServiceManufacturerById(uuid));
    }

    @Operation(summary = "Gets all TakerServiceManufacturers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns all TakerServiceManufacturers")
    })
    @GetMapping
    public List<TakerServiceManufacturerDto> getAllTakerServiceManufacturers() {
        return DomainToDtoConverter.convert(takerServiceManufacturerService.getAllTakerServiceManufacturers());
    }

    @Operation(summary = "Deletes a TakerServiceManufacturer by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200")
    })
    @DeleteMapping("/{uuid}")
    public void deleteTakerServiceManufacturer(@PathVariable UUID uuid) {
        takerServiceManufacturerService.deleteTakerServiceManufacturer(uuid);
    }
}
