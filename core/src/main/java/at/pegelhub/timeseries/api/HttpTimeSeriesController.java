package at.pegelhub.timeseries.api;

import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.timeseries.application.TimeSeriesService;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@RestController
@RequestMapping("/api/v1/time-series")
@Tag(name = "Time Series", description = "openapi.timeseries.http-time-series-controller.manage-observed-time-series-metadata")
@SecurityRequirement(name = "bearerAuth")
final class HttpTimeSeriesController {

    private final TimeSeriesService timeSeries;

    HttpTimeSeriesController(TimeSeriesService timeSeries) {
        this.timeSeries = requireNonNull(timeSeries);
    }

    @Operation(
            summary = "openapi.timeseries.http-time-series-controller.creates-a-time-series",
            description = "openapi.timeseries.http-time-series-controller.creates-observed-property-metadata-for-a-measuring")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "openapi.timeseries.http-time-series-controller.returns-the-created-time-series",
                    content = @Content(schema = @Schema(implementation = TimeSeriesResponse.class))),
            @ApiResponse(responseCode = "400", description = "openapi.timeseries.http-time-series-controller.the-time-series-payload-is-invalid", content = @Content),
            @ApiResponse(responseCode = "404", description = "openapi.timeseries.http-time-series-controller.a-referenced-measuring-point-or-connector-was", content = @Content),
            @ApiResponse(responseCode = "409", description = "openapi.shared.metadata-conflict", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TimeSeriesResponse create(@Valid @RequestBody CreateTimeSeriesRequest request) {
        return TimeSeriesMapper.toResponse(timeSeries.create(TimeSeriesMapper.toCommand(request)));
    }

    @Operation(
            summary = "openapi.timeseries.http-time-series-controller.updates-a-time-series",
            description = "openapi.timeseries.http-time-series-controller.replaces-time-series-mapping-metadata-requires-metadata-write")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "openapi.timeseries.http-time-series-controller.returns-the-updated-time-series",
                    content = @Content(schema = @Schema(implementation = TimeSeriesResponse.class))),
            @ApiResponse(responseCode = "400", description = "openapi.timeseries.http-time-series-controller.the-time-series-payload-is-invalid", content = @Content),
            @ApiResponse(responseCode = "404", description = "openapi.timeseries.http-time-series-controller.the-time-series-or-source-connector-was-not-found", content = @Content),
            @ApiResponse(responseCode = "409", description = "openapi.shared.metadata-conflict", content = @Content)
    })
    @PutMapping("/{id}")
    TimeSeriesResponse update(
            @Parameter(description = "openapi.measurement.measurement-api.time-series-identifier", required = true)
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateTimeSeriesRequest request) {
        return TimeSeriesMapper.toResponse(timeSeries.update(
                new TimeSeriesId(id),
                TimeSeriesMapper.toCommand(request)));
    }

    @Operation(
            summary = "openapi.timeseries.http-time-series-controller.gets-a-time-series-by-id",
            description = "openapi.timeseries.http-time-series-controller.returns-observed-time-series-metadata-requires-metadata")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "openapi.timeseries.http-time-series-controller.returns-the-time-series",
                    content = @Content(schema = @Schema(implementation = TimeSeriesResponse.class))),
            @ApiResponse(responseCode = "400", description = "openapi.timeseries.http-time-series-controller.the-time-series-uuid-is-invalid", content = @Content),
            @ApiResponse(responseCode = "404", description = "openapi.timeseries.http-time-series-controller.the-time-series-was-not-found", content = @Content)
    })
    @GetMapping("/{id}")
    TimeSeriesResponse get(@Parameter(description = "openapi.measurement.measurement-api.time-series-identifier", required = true) @PathVariable UUID id) {
        return TimeSeriesMapper.toResponse(timeSeries.get(new TimeSeriesId(id)));
    }

    @Operation(
            summary = "openapi.timeseries.http-time-series-controller.lists-time-series",
            description = "openapi.timeseries.http-time-series-controller.returns-all-time-series-optionally-filtered-by")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "openapi.timeseries.http-time-series-controller.returns-time-series-records",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TimeSeriesResponse.class)))),
            @ApiResponse(responseCode = "400", description = "openapi.timeseries.http-time-series-controller.a-query-parameter-is-invalid-or-both", content = @Content),
            @ApiResponse(responseCode = "404", description = "openapi.timeseries.http-time-series-controller.the-measuring-point-or-station-was-not", content = @Content)
    })
    @GetMapping
    List<TimeSeriesResponse> list(
            @Parameter(description = "openapi.timeseries.http-time-series-controller.optional-measuring-point-identifier-to-filter-time", required = false)
            @RequestParam(required = false) UUID measuringPointId,
            @Parameter(description = "openapi.timeseries.http-time-series-controller.optional-station-identifier-to-filter-time-series", required = false)
            @RequestParam(required = false) UUID stationId) {
        if (measuringPointId != null && stationId != null) {
            throw new IllegalArgumentException("Provide either measuringPointId or stationId");
        }
        var result = measuringPointId != null
                ? timeSeries.listForMeasuringPoint(new MeasuringPointId(measuringPointId))
                : stationId != null
                        ? timeSeries.listForStation(new StationId(stationId))
                        : timeSeries.list();
        return result.stream()
                .map(TimeSeriesMapper::toResponse)
                .toList();
    }
}
