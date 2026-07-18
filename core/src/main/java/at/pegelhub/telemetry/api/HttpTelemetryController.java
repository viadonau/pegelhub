package at.pegelhub.telemetry.api;

import at.pegelhub.telemetry.application.TelemetryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * REST controller for telemetry.
 */
@RestController
@RequestMapping("/api/v1/telemetry")
@Tag(name = "Telemetry", description = "Write and query connector technical telemetry.")
@SecurityRequirement(name = "bearerAuth")
public class HttpTelemetryController {

    private final TelemetryService telemetryService;

    public HttpTelemetryController(TelemetryService telemetryService) {
        this.telemetryService = requireNonNull(telemetryService);
    }

    @Operation(
            summary = "Writes telemetry",
            description = "Stores one technical telemetry entry. Requires TELEMETRY_WRITE or SYSTEM_ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns the saved telemetry entry",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = TelemetryResponse.class))}),
            @ApiResponse(responseCode = "400", description = "The telemetry payload is invalid.", content = @Content),
            @ApiResponse(responseCode = "404", description = "The authenticated client has no registered connector.", content = @Content)
    })
    @PostMapping
    public TelemetryResponse writeTelemetryData(@RequestBody TelemetryWriteRequest telemetry) {
        return TelemetryMapper.toResponse(telemetryService.saveTelemetry(TelemetryMapper.toCommand(telemetry)));
    }

    @Operation(
            summary = "Lists telemetry in a relative range",
            description = "Returns telemetry entries in a positive relative range such as 72h. Requires TELEMETRY_READ or SYSTEM_ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns telemetry entries in the requested range.",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TelemetryResponse.class)))),
            @ApiResponse(responseCode = "400", description = "The range is invalid.", content = @Content),
            @ApiResponse(responseCode = "500", description = "The telemetry entries could not be read.", content = @Content)
    })
    @GetMapping("/{range}")
    public List<TelemetryResponse> findTelemetryInRange(
            @Parameter(description = "Positive relative range such as 72h.", example = "72h", required = true)
            @PathVariable String range) {
        return TelemetryMapper.toResponses(telemetryService.getByRange(range));
    }

    @Operation(
            summary = "Gets the latest telemetry entry for an ID",
            description = "Returns the most recent telemetry entry for a measurement/station identifier. Requires TELEMETRY_READ or SYSTEM_ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns the telemetry entry",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = TelemetryResponse.class))}),
            @ApiResponse(responseCode = "400", description = "The UUID is invalid.", content = @Content),
            @ApiResponse(responseCode = "500", description = "The latest telemetry entry could not be resolved.", content = @Content)
    })
    @GetMapping("/last/{uuid}")
    public TelemetryResponse findTelemetryById(
            @Parameter(description = "Measurement or station telemetry identifier.", required = true)
            @PathVariable UUID uuid) {
        return TelemetryMapper.toResponse(telemetryService.getLastData(uuid));
    }
}
