package at.pegelhub.measuringpoint.api;

import at.pegelhub.measuringpoint.application.MeasuringPointService;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.station.domain.StationId;
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
@RequestMapping("/api/v1/measuring-points")
@Tag(name = "Measuring Points", description = "openapi.measuringpoint.http-measuring-point-controller.manage-physical-measuring-points-within-stations")
@SecurityRequirement(name = "bearerAuth")
final class HttpMeasuringPointController {

    private final MeasuringPointService measuringPoints;

    HttpMeasuringPointController(MeasuringPointService measuringPoints) {
        this.measuringPoints = requireNonNull(measuringPoints);
    }

    @Operation(
            summary = "openapi.measuringpoint.http-measuring-point-controller.creates-a-measuring-point",
            description = "openapi.measuringpoint.http-measuring-point-controller.creates-physical-measuring-point-metadata-under-a")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "openapi.measuringpoint.http-measuring-point-controller.returns-the-created-measuring-point",
                    content = @Content(schema = @Schema(implementation = MeasuringPointResponse.class))),
            @ApiResponse(responseCode = "400", description = "openapi.measuringpoint.http-measuring-point-controller.the-measuring-point-payload-is-invalid", content = @Content),
            @ApiResponse(responseCode = "404", description = "openapi.measuringpoint.http-measuring-point-controller.the-station-was-not-found", content = @Content),
            @ApiResponse(responseCode = "409", description = "openapi.shared.metadata-conflict", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    MeasuringPointResponse create(@Valid @RequestBody CreateMeasuringPointRequest request) {
        return MeasuringPointMapper.toResponse(measuringPoints.create(MeasuringPointMapper.toCommand(request)));
    }

    @Operation(
            summary = "openapi.measuringpoint.http-measuring-point-controller.updates-a-measuring-point",
            description = "openapi.measuringpoint.http-measuring-point-controller.replaces-measuring-point-metadata-requires-metadata-write")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "openapi.measuringpoint.http-measuring-point-controller.returns-the-updated-measuring-point",
                    content = @Content(schema = @Schema(implementation = MeasuringPointResponse.class))),
            @ApiResponse(responseCode = "400", description = "openapi.measuringpoint.http-measuring-point-controller.the-measuring-point-payload-is-invalid", content = @Content),
            @ApiResponse(responseCode = "404", description = "openapi.measuringpoint.http-measuring-point-controller.the-measuring-point-was-not-found", content = @Content),
            @ApiResponse(responseCode = "409", description = "openapi.shared.metadata-conflict", content = @Content)
    })
    @PutMapping("/{id}")
    MeasuringPointResponse update(
            @Parameter(description = "openapi.measuringpoint.http-measuring-point-controller.measuring-point-identifier", required = true)
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateMeasuringPointRequest request) {
        return MeasuringPointMapper.toResponse(measuringPoints.update(
                new MeasuringPointId(id),
                MeasuringPointMapper.toCommand(request)));
    }

    @Operation(
            summary = "openapi.measuringpoint.http-measuring-point-controller.gets-a-measuring-point-by-id",
            description = "openapi.measuringpoint.http-measuring-point-controller.returns-measuring-point-metadata-requires-metadata-read")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "openapi.measuringpoint.http-measuring-point-controller.returns-the-measuring-point",
                    content = @Content(schema = @Schema(implementation = MeasuringPointResponse.class))),
            @ApiResponse(responseCode = "400", description = "openapi.measuringpoint.http-measuring-point-controller.the-measuring-point-uuid-is-invalid", content = @Content),
            @ApiResponse(responseCode = "404", description = "openapi.measuringpoint.http-measuring-point-controller.the-measuring-point-was-not-found", content = @Content)
    })
    @GetMapping("/{id}")
    MeasuringPointResponse get(
            @Parameter(description = "openapi.measuringpoint.http-measuring-point-controller.measuring-point-identifier", required = true) @PathVariable UUID id) {
        return MeasuringPointMapper.toResponse(measuringPoints.get(new MeasuringPointId(id)));
    }

    @Operation(
            summary = "openapi.measuringpoint.http-measuring-point-controller.lists-measuring-points",
            description = "openapi.measuringpoint.http-measuring-point-controller.returns-all-measuring-points-optionally-filtered-by")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "openapi.measuringpoint.http-measuring-point-controller.returns-measuring-point-records",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = MeasuringPointResponse.class)))),
            @ApiResponse(responseCode = "400", description = "openapi.measuringpoint.http-measuring-point-controller.the-station-id-query-parameter-is-invalid", content = @Content),
            @ApiResponse(responseCode = "404", description = "openapi.measuringpoint.http-measuring-point-controller.the-station-was-not-found", content = @Content)
    })
    @GetMapping
    List<MeasuringPointResponse> list(
            @Parameter(description = "openapi.measuringpoint.http-measuring-point-controller.optional-station-identifier-to-filter-measuring-points", required = false)
            @RequestParam(required = false) UUID stationId) {
        var result = stationId == null
                ? measuringPoints.list()
                : measuringPoints.listForStation(new StationId(stationId));
        return result.stream()
                .map(MeasuringPointMapper::toResponse)
                .toList();
    }
}
