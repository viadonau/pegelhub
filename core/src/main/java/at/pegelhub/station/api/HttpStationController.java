package at.pegelhub.station.api;

import at.pegelhub.station.application.StationService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@RestController
@RequestMapping("/api/v1/stations")
@Tag(name = "Stations", description = "openapi.station.http-station-controller.manage-hydrological-stations")
@SecurityRequirement(name = "bearerAuth")
final class HttpStationController {

    private final StationService stations;

    HttpStationController(StationService stations) {
        this.stations = requireNonNull(stations);
    }

    @Operation(
            summary = "openapi.station.http-station-controller.creates-a-station",
            description = "openapi.station.http-station-controller.creates-station-metadata-under-a-station-owner")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "openapi.station.http-station-controller.returns-the-created-station",
                    content = @Content(schema = @Schema(implementation = StationResponse.class))),
            @ApiResponse(responseCode = "400", description = "openapi.station.http-station-controller.the-station-payload-is-invalid", content = @Content),
            @ApiResponse(responseCode = "404", description = "openapi.station.http-station-controller.the-station-owner-was-not-found", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    StationResponse create(@Valid @RequestBody CreateStationRequest request) {
        return StationMapper.toResponse(stations.create(StationMapper.toCommand(request)));
    }

    @Operation(
            summary = "openapi.station.http-station-controller.gets-a-station-by-id",
            description = "openapi.station.http-station-controller.returns-station-metadata-requires-metadata-read-metadata")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "openapi.station.http-station-controller.returns-the-station",
                    content = @Content(schema = @Schema(implementation = StationResponse.class))),
            @ApiResponse(responseCode = "400", description = "openapi.station.http-station-controller.the-station-uuid-is-invalid", content = @Content),
            @ApiResponse(responseCode = "404", description = "openapi.measuringpoint.http-measuring-point-controller.the-station-was-not-found", content = @Content)
    })
    @GetMapping("/{id}")
    StationResponse get(@Parameter(description = "openapi.station.http-station-controller.station-identifier", required = true) @PathVariable UUID id) {
        return StationMapper.toResponse(stations.get(new StationId(id)));
    }

    @Operation(
            summary = "openapi.station.http-station-controller.lists-stations",
            description = "openapi.station.http-station-controller.returns-all-station-metadata-records-requires-metadata")
    @ApiResponse(
            responseCode = "200",
            description = "openapi.station.http-station-controller.returns-stations",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = StationResponse.class))))
    @GetMapping
    List<StationResponse> list() {
        return stations.list().stream()
                .map(StationMapper::toResponse)
                .toList();
    }
}
