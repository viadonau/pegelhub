package at.pegelhub.telemetry.api;

import at.pegelhub.telemetry.domain.Telemetry;
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
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * REST controller for telemetry.
 */
@RestController
@RequestMapping("/api/v1/telemetry")
@Tag(name = "Telemetry", description = "openapi.telemetry.http-telemetry-controller.write-and-query-connector-technical-telemetry")
@SecurityRequirement(name = "bearerAuth")
public class HttpTelemetryController {

    private final TelemetryService telemetryService;

    public HttpTelemetryController(TelemetryService telemetryService) {
        this.telemetryService = requireNonNull(telemetryService);
    }

    @Operation(
            summary = "openapi.telemetry.http-telemetry-controller.writes-telemetry",
            description = "openapi.telemetry.http-telemetry-controller.stores-one-technical-telemetry-entry-requires-telemetry")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "openapi.telemetry.http-telemetry-controller.returns-the-saved-telemetry-entry",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Telemetry.class))}),
            @ApiResponse(responseCode = "400", description = "openapi.telemetry.http-telemetry-controller.the-telemetry-payload-is-invalid", content = @Content),
            @ApiResponse(responseCode = "404", description = "openapi.telemetry.http-telemetry-controller.the-authenticated-client-has-no-registered-connector", content = @Content)
    })
    @PostMapping
    public Telemetry writeTelemetryData(@Valid @RequestBody WriteTelemetryRequest request) {
        return telemetryService.saveTelemetry(TelemetryMapper.toCommand(request));
    }

    @Operation(
            summary = "openapi.telemetry.http-telemetry-controller.lists-telemetry-in-a-relative-range",
            description = "openapi.telemetry.http-telemetry-controller.returns-telemetry-entries-in-a-positive-relative")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "openapi.telemetry.http-telemetry-controller.returns-telemetry-entries-in-the-requested-range",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Telemetry.class)))),
            @ApiResponse(responseCode = "400", description = "openapi.telemetry.http-telemetry-controller.the-range-is-invalid", content = @Content)
    })
    @GetMapping("/{range}")
    public List<Telemetry> findTelemetryInRange(
            @Parameter(description = "openapi.telemetry.http-telemetry-controller.positive-relative-range-such-as-72h", example = "72h", required = true)
            @PathVariable String range) {
        return telemetryService.getByRange(range);
    }

    @Operation(
            summary = "openapi.telemetry.http-telemetry-controller.gets-the-latest-telemetry-entry-for-an",
            description = "openapi.telemetry.http-telemetry-controller.returns-the-most-recent-telemetry-entry-for")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "openapi.telemetry.http-telemetry-controller.returns-the-telemetry-entry",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Telemetry.class))}),
            @ApiResponse(responseCode = "400", description = "openapi.telemetry.http-telemetry-controller.the-uuid-is-invalid", content = @Content),
            @ApiResponse(responseCode = "500", description = "openapi.telemetry.http-telemetry-controller.the-latest-telemetry-entry-could-not-be", content = @Content)
    })
    @GetMapping("/last/{uuid}")
    public Telemetry findTelemetryById(
            @Parameter(description = "openapi.telemetry.http-telemetry-controller.measurement-or-station-telemetry-identifier", required = true)
            @PathVariable UUID uuid) {
        return telemetryService.getLastData(uuid);
    }
}
