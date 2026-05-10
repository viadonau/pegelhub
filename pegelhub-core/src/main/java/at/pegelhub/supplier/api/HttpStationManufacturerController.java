package at.pegelhub.supplier.api;

import at.pegelhub.shared.web.DomainToDtoConverter;
import at.pegelhub.shared.web.DtoToDomainConverter;
import at.pegelhub.shared.web.*;

import at.pegelhub.supplier.application.StationManufacturerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * REST controller for station manufacturers.
 */
@RestController
@RequestMapping("/api/v1/stationManufacturer")
public class HttpStationManufacturerController {

    private final StationManufacturerService stationManufacturerService;

    public HttpStationManufacturerController(StationManufacturerService stationManufacturerService) {
        this.stationManufacturerService = requireNonNull(stationManufacturerService);
    }

    @Operation(summary = "Saves a StationManufacturer to the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns the saved StationManufacturer")
    })
    @PostMapping
    public StationManufacturerDto saveStationManufacturer(@RequestBody CreateStationManufacturerDto stationManufacturer) {
        return DomainToDtoConverter.convert(stationManufacturerService.createStationManufacturer(DtoToDomainConverter.convert(stationManufacturer)));
    }

    @Operation(summary = "Gets a StationManufacturer by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns the StationManufacturer")
    })
    @GetMapping("/{uuid}")
    public StationManufacturerDto getStationManufacturerById(@PathVariable UUID uuid) {
        return DomainToDtoConverter.convert(stationManufacturerService.getStationManufacturerById(uuid));
    }

    @Operation(summary = "Gets all StationManufacturers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns all StationManufacturers")
    })
    @GetMapping
    public List<StationManufacturerDto> getAllStationManufacturers() {
        return DomainToDtoConverter.convert(stationManufacturerService.getAllStationManufacturers());
    }

    @Operation(summary = "Deletes a StationManufacturer by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200")
    })
    @DeleteMapping("/{uuid}")
    public void deleteStationManufacturer(@PathVariable UUID uuid) {
        stationManufacturerService.deleteStationManufacturer(uuid);
    }
}
