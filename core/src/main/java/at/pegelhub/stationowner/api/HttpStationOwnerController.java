package at.pegelhub.stationowner.api;

import at.pegelhub.stationowner.application.StationOwnerService;
import at.pegelhub.stationowner.domain.StationOwnerId;
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
@RequestMapping("/api/v1/station-owners")
@Tag(name = "Station Owners", description = "Manage station owner metadata.")
@SecurityRequirement(name = "bearerAuth")
final class HttpStationOwnerController {

    private final StationOwnerService stationOwners;

    HttpStationOwnerController(StationOwnerService stationOwners) {
        this.stationOwners = requireNonNull(stationOwners);
    }

    @Operation(
            summary = "Creates a station owner",
            description = "Creates station owner metadata. Requires METADATA_WRITE or SYSTEM_ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Returns the created station owner.",
                    content = @Content(schema = @Schema(implementation = StationOwnerResponse.class))),
            @ApiResponse(responseCode = "400", description = "The station owner payload is invalid.", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    StationOwnerResponse create(@Valid @RequestBody CreateStationOwnerRequest request) {
        return StationOwnerMapper.toResponse(stationOwners.create(StationOwnerMapper.toCommand(request)));
    }

    @Operation(
            summary = "Gets a station owner by ID",
            description = "Returns station owner metadata. Requires METADATA_READ, METADATA_WRITE, or SYSTEM_ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Returns the station owner.",
                    content = @Content(schema = @Schema(implementation = StationOwnerResponse.class))),
            @ApiResponse(responseCode = "400", description = "The station owner UUID is invalid.", content = @Content),
            @ApiResponse(responseCode = "404", description = "The station owner was not found.", content = @Content)
    })
    @GetMapping("/{id}")
    StationOwnerResponse get(@Parameter(description = "Station owner identifier.", required = true) @PathVariable UUID id) {
        return StationOwnerMapper.toResponse(stationOwners.get(new StationOwnerId(id)));
    }

    @Operation(
            summary = "Lists station owners",
            description = "Returns all station owner metadata records. Requires METADATA_READ, METADATA_WRITE, or SYSTEM_ADMIN.")
    @ApiResponse(
            responseCode = "200",
            description = "Returns station owners.",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = StationOwnerResponse.class))))
    @GetMapping
    List<StationOwnerResponse> list() {
        return stationOwners.list().stream()
                .map(StationOwnerMapper::toResponse)
                .toList();
    }
}
