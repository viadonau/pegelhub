package at.pegelhub.measurement.api;

import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.measurement.application.MeasurementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static at.pegelhub.measurement.api.DtoToDomainConverter.convert;
import static java.util.Objects.requireNonNull;

/**
 * REST controller for measurements.
 */
@RestController
@RequestMapping("/api/v1/measurement")
public class HttpMeasurementController {

    private final MeasurementService measurementService;

    public HttpMeasurementController(MeasurementService measurementService) {
        this.measurementService = requireNonNull(measurementService);
    }

    @Operation(summary = "Adds a new Measurement Entry to the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "The given measurement was successfully created.")
    })
    @PostMapping
    public synchronized void writeMeasurementData(@RequestBody WriteMeasurementsDto measurements) {
        measurementService.writeMeasurements(convert(measurements));
    }

    @Operation(summary = "Gets all Measurement Data in Range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns all Measurement Data in Range",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Measurement.class))})
    })
    @GetMapping("/{range}")
    public List<Measurement> findMeasurementInRange(@PathVariable String range) {
        return measurementService.getByRange(range);
    }

    @Operation(summary = "Gets all Measurement Data for Supplier In Range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns all Measurement Data for Supplier",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Measurement.class))})
    })
    @GetMapping("/supplier/{range}")
    public List<Measurement> findMeasurementForSupplierInRange(
            @RequestParam(name = "stationNumber", defaultValue = "") String stationNumber,
            @PathVariable String range) {
        return measurementService.getBySupplierAndRange(stationNumber, range);
    }

    @Operation(summary = "Gets latest Measurement from Supplier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns the latest measurement",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Measurement.class))})
    })
    @GetMapping("/supplier/latest")
    public Measurement findLatestMeasurementBySupplier(
            @RequestParam(name = "stationNumber", defaultValue = "") String stationNumber) {
        return measurementService.getLatestBySupplier(stationNumber);
    }

    @Operation(summary = "Gets the average of all measurement fields from a Supplier over a given time range.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns a single Measurement object where each field is the calculated average over the time range.",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Measurement.class))}),
            @ApiResponse(responseCode = "404", description = "No supplier found for the given station number or no data available in the specified range.")
    })
    @GetMapping("/supplier/average/{range}")
    public Measurement findAverageMeasurementForSupplierInRange(
            @RequestParam(name = "stationNumber") String stationNumber,
            @PathVariable String range) {
        return measurementService.getAverageBySupplierAndRange(stationNumber, range);
    }

    @Operation(summary = "Gets last Measurement entry for ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns the measurement entry",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Measurement.class))})
    })
    @GetMapping("/last/{uuid}")
    public Measurement findMeasurementById(@PathVariable UUID uuid) {
        return measurementService.getLastData(uuid);
    }

    @GetMapping("/systemTime")
    public Instant getSystemtime() {
        return measurementService.getSystemTime();
    }

}
