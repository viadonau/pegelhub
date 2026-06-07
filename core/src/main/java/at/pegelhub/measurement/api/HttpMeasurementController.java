package at.pegelhub.measurement.api;

import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.measurement.application.MeasurementService;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
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
    public synchronized void writeMeasurementData(@Valid @RequestBody WriteMeasurementsDto measurements) {
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

    @Operation(summary = "Gets all Measurement Data for TimeSeries In Range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns all Measurement Data for TimeSeries",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Measurement.class))})
    })
    @GetMapping("/time-series/{timeSeriesId}/{range}")
    public List<Measurement> findMeasurementForTimeSeriesInRange(
            @PathVariable UUID timeSeriesId,
            @PathVariable String range) {
        return measurementService.getByTimeSeriesAndRange(new TimeSeriesId(timeSeriesId), range);
    }

    @Operation(summary = "Gets latest Measurement from TimeSeries")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns the latest measurement",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Measurement.class))})
    })
    @GetMapping("/time-series/{timeSeriesId}/latest")
    public Measurement findLatestMeasurementByTimeSeries(@PathVariable UUID timeSeriesId) {
        return measurementService.getLatestByTimeSeries(new TimeSeriesId(timeSeriesId));
    }

    @Operation(summary = "Gets the average Measurement value from a TimeSeries over a given time range.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns a single Measurement object with the calculated average value over the time range.",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Measurement.class))}),
            @ApiResponse(responseCode = "404", description = "No TimeSeries found or no data available in the specified range.")
    })
    @GetMapping("/time-series/{timeSeriesId}/average/{range}")
    public Measurement findAverageMeasurementForTimeSeriesInRange(
            @PathVariable UUID timeSeriesId,
            @PathVariable String range) {
        return measurementService.getAverageByTimeSeriesAndRange(new TimeSeriesId(timeSeriesId), range);
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
