package at.pegelhub.timeseries.api;

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
@Tag(name = "Time Series", description = "Manage observed time series metadata.")
@SecurityRequirement(name = "bearerAuth")
final class HttpTimeSeriesController {

    private final TimeSeriesService timeSeries;

    HttpTimeSeriesController(TimeSeriesService timeSeries) {
        this.timeSeries = requireNonNull(timeSeries);
    }

    @Operation(
            summary = "Creates a time series",
            description = "Creates observed property metadata for a station. Requires METADATA_WRITE or SYSTEM_ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Returns the created time series.",
                    content = @Content(schema = @Schema(implementation = TimeSeriesResponse.class))),
            @ApiResponse(responseCode = "400", description = "The time series payload is invalid.", content = @Content),
            @ApiResponse(responseCode = "404", description = "A referenced station or connector was not found.", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TimeSeriesResponse create(@Valid @RequestBody CreateTimeSeriesRequest request) {
        return TimeSeriesMapper.toResponse(timeSeries.create(TimeSeriesMapper.toCommand(request)));
    }

    @Operation(
            summary = "Gets a time series by ID",
            description = "Returns observed time series metadata. Requires METADATA_READ, METADATA_WRITE, or SYSTEM_ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Returns the time series.",
                    content = @Content(schema = @Schema(implementation = TimeSeriesResponse.class))),
            @ApiResponse(responseCode = "400", description = "The time series UUID is invalid.", content = @Content),
            @ApiResponse(responseCode = "404", description = "The time series was not found.", content = @Content)
    })
    @GetMapping("/{id}")
    TimeSeriesResponse get(@Parameter(description = "Time series identifier.", required = true) @PathVariable UUID id) {
        return TimeSeriesMapper.toResponse(timeSeries.get(new TimeSeriesId(id)));
    }

    @Operation(
            summary = "Lists time series",
            description = "Returns all time series, optionally filtered by stationId. Requires METADATA_READ, METADATA_WRITE, or SYSTEM_ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Returns time series records.",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TimeSeriesResponse.class)))),
            @ApiResponse(responseCode = "400", description = "The stationId query parameter is invalid.", content = @Content)
    })
    @GetMapping
    List<TimeSeriesResponse> list(
            @Parameter(description = "Optional station identifier to filter time series.", required = false)
            @RequestParam(required = false) UUID stationId) {
        var result = stationId == null
                ? timeSeries.list()
                : timeSeries.listForStation(new StationId(stationId));
        return result.stream()
                .map(TimeSeriesMapper::toResponse)
                .toList();
    }
}
