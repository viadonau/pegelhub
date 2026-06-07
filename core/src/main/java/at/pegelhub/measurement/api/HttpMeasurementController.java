package at.pegelhub.measurement.api;

import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.measurement.domain.MeasurementAverage;
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

@RestController
@RequestMapping("/api/v1")
public class HttpMeasurementController {

    private final MeasurementService measurementService;

    public HttpMeasurementController(MeasurementService measurementService) {
        this.measurementService = requireNonNull(measurementService);
    }

    @Operation(summary = "Writes measurements for one or more time series")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "The measurements were successfully written.")
    })
    @PostMapping("/measurements")
    public void writeMeasurementData(@Valid @RequestBody WriteMeasurementsDto measurements) {
        measurementService.writeMeasurements(convert(measurements));
    }

    @Operation(summary = "Gets all measurements for a time series within a range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns all measurements for the time series in the range",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Measurement.class))})
    })
    @GetMapping("/time-series/{timeSeriesId}/measurements/{range}")
    public List<Measurement> findMeasurementsInRange(
            @PathVariable UUID timeSeriesId,
            @PathVariable String range) {
        return measurementService.getByTimeSeriesAndRange(new TimeSeriesId(timeSeriesId), range);
    }

    @Operation(summary = "Gets the latest measurement for a time series")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns the latest measurement",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Measurement.class))})
    })
    @GetMapping("/time-series/{timeSeriesId}/measurements/latest")
    public Measurement findLatestMeasurement(@PathVariable UUID timeSeriesId) {
        return measurementService.getLatestByTimeSeries(new TimeSeriesId(timeSeriesId));
    }

    @Operation(summary = "Gets the average measurement value for a time series over a range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns the averaged measurement",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = MeasurementAverage.class))}),
            @ApiResponse(responseCode = "404", description = "No data available in the specified range.")
    })
    @GetMapping("/time-series/{timeSeriesId}/measurements/average/{range}")
    public MeasurementAverage findAverageMeasurement(
            @PathVariable UUID timeSeriesId,
            @PathVariable String range) {
        return measurementService.getAverageByTimeSeriesAndRange(new TimeSeriesId(timeSeriesId), range);
    }

    @GetMapping("/measurements/system-time")
    public Instant getSystemTime() {
        return measurementService.getSystemTime();
    }

}
