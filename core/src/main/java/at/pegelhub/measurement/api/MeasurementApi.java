package at.pegelhub.measurement.api;

import at.pegelhub.measurement.api.read.input.MeasurementBucketParameters;
import at.pegelhub.measurement.api.read.input.MeasurementReadParameters;
import at.pegelhub.measurement.api.read.output.MeasurementBucketListResponse;
import at.pegelhub.measurement.api.read.output.MeasurementListResponse;
import at.pegelhub.measurement.api.write.WriteMeasurementsRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
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
@Tag(name = "Measurements", description = "openapi.measurement.measurement-api.write-and-read-time-series-measurements")
@Tag(name = "Measurement Buckets", description = "openapi.measurement.measurement-api.read-chart-ready-aggregated-measurement-buckets")
public interface MeasurementApi {

    @Operation(
            tags = "Measurements",
            summary = "openapi.measurement.measurement-api.writes-measurements-for-one-or-more-time",
            description = "openapi.measurement.measurement-api.write.description",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "openapi.measurement.measurement-api.the-measurements-were-successfully-written"),
            @ApiResponse(responseCode = "400", description = "openapi.measurement.measurement-api.the-request-body-is-invalid", content = @Content),
            @ApiResponse(responseCode = "404", description = "openapi.measurement.measurement-api.a-connector-or-time-series-was-not", content = @Content)
    })
    @PostMapping("/measurements")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void writeMeasurementData(@Valid @RequestBody WriteMeasurementsRequest measurements);

    @Operation(
            tags = "Measurements",
            summary = "openapi.measurement.measurement-api.lists-raw-measurements-for-a-time-series",
            description = "openapi.measurement.measurement-api.list-raw.description",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "openapi.measurement.measurement-api.returns-a-lean-envelope-of-raw-measurements",
                    content = @Content(schema = @Schema(implementation = MeasurementListResponse.class))),
            @ApiResponse(responseCode = "400", description = "openapi.measurement.measurement-api.the-query-parameters-are-invalid", content = @Content),
            @ApiResponse(responseCode = "404", description = "openapi.measurement.measurement-api.the-connector-or-time-series-was-not", content = @Content)
    })
    @GetMapping("/time-series/{timeSeriesId}/measurements")
    MeasurementListResponse listMeasurements(
            @Parameter(description = "openapi.measurement.measurement-api.time-series-identifier", required = true)
            @PathVariable UUID timeSeriesId,
            @ParameterObject @Valid @ModelAttribute MeasurementReadParameters parameters);

    @Operation(
            tags = "Measurement Buckets",
            summary = "openapi.measurement.measurement-api.lists-chart-ready-measurement-buckets-for-a",
            description = "openapi.measurement.measurement-api.list-buckets.description",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "openapi.measurement.measurement-api.returns-average-buckets-for-charting",
                    content = @Content(schema = @Schema(implementation = MeasurementBucketListResponse.class))),
            @ApiResponse(responseCode = "400", description = "openapi.measurement.measurement-api.the-query-parameters-are-invalid", content = @Content),
            @ApiResponse(responseCode = "404", description = "openapi.measurement.measurement-api.the-connector-or-time-series-was-not", content = @Content)
    })
    @GetMapping("/time-series/{timeSeriesId}/measurements/buckets")
    MeasurementBucketListResponse listMeasurementBuckets(
            @Parameter(description = "openapi.measurement.measurement-api.time-series-identifier", required = true)
            @PathVariable UUID timeSeriesId,
            @ParameterObject @Valid @ModelAttribute MeasurementBucketParameters parameters);

    @Operation(
            tags = "Measurements",
            summary = "openapi.measurement.measurement-api.reads-the-measurement-database-system-time",
            description = "openapi.measurement.measurement-api.returns-the-influx-db-system-time-used")
    @ApiResponse(
            responseCode = "200",
            description = "openapi.measurement.measurement-api.returns-the-database-system-time",
            content = @Content(schema = @Schema(type = "string", format = "date-time", example = "2026-06-17T13:00:00Z")))
    @GetMapping("/measurements/system-time")
    Instant getSystemTime();
}
