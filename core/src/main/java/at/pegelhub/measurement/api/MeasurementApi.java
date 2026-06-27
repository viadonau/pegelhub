package at.pegelhub.measurement.api;

import at.pegelhub.measurement.api.read.input.MeasurementBucketParameters;
import at.pegelhub.measurement.api.read.input.MeasurementPageParameters;
import at.pegelhub.measurement.api.read.output.MeasurementBucketListResponse;
import at.pegelhub.measurement.api.read.output.MeasurementListResponse;
import at.pegelhub.measurement.api.write.WriteMeasurementsRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.Instant;
import java.util.UUID;

@RequestMapping("/api/v1")
public interface MeasurementApi {

    @Operation(
            summary = "Writes measurements for one or more time series",
            description = """
                    Stores raw measurements submitted by the authenticated connector.
                    The connector identity is taken from the access token; submittedByConnectorId is not accepted from the request body.
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "The measurements were successfully written."),
            @ApiResponse(responseCode = "400", description = "The request body is invalid.", content = @Content),
            @ApiResponse(responseCode = "403", description = "The connector is not allowed to write one or more time series.", content = @Content),
            @ApiResponse(responseCode = "404", description = "A connector or time series was not found.", content = @Content)
    })
    @PostMapping("/measurements")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void writeMeasurementData(@Valid @RequestBody WriteMeasurementsRequest measurements);

    @Operation(
            summary = "Lists raw measurements for a time series",
            description = """
                    Returns raw measurement points in a bounded time window.
                    Provide either last or both from and to. Cursor pagination uses an opaque stable position and must be reused with the same resolved window and order.
                    For the latest measurement, request a bounded descending list with limit=1, for example last=365d&order=desc&limit=1.
                    """)
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Returns a lean envelope of raw measurements.",
                    content = @Content(schema = @Schema(implementation = MeasurementListResponse.class))),
            @ApiResponse(responseCode = "400", description = "The query parameters are invalid.", content = @Content),
            @ApiResponse(responseCode = "403", description = "The connector is not allowed to read the time series.", content = @Content),
            @ApiResponse(responseCode = "404", description = "The connector or time series was not found.", content = @Content)
    })
    @GetMapping("/time-series/{timeSeriesId}/measurements")
    MeasurementListResponse listMeasurements(
            @Parameter(description = "Time series identifier.", required = true)
            @PathVariable UUID timeSeriesId,
            @Valid @ModelAttribute MeasurementPageParameters parameters);

    @Operation(
            summary = "Lists chart-ready measurement buckets for a time series",
            description = """
                    Returns average buckets for charting in a bounded time window.
                    Provide either last or both from and to. Supply bucket for an explicit width, or maxPoints for automatic resolution.
                    """)
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Returns average buckets for charting.",
                    content = @Content(schema = @Schema(implementation = MeasurementBucketListResponse.class))),
            @ApiResponse(responseCode = "400", description = "The query parameters are invalid.", content = @Content),
            @ApiResponse(responseCode = "403", description = "The connector is not allowed to read the time series.", content = @Content),
            @ApiResponse(responseCode = "404", description = "The connector or time series was not found.", content = @Content)
    })
    @GetMapping("/time-series/{timeSeriesId}/measurements/buckets")
    MeasurementBucketListResponse listMeasurementBuckets(
            @Parameter(description = "Time series identifier.", required = true)
            @PathVariable UUID timeSeriesId,
            @Valid @ModelAttribute MeasurementBucketParameters parameters);

    @GetMapping("/measurements/system-time")
    Instant getSystemTime();
}
