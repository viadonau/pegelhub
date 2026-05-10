package com.stm.pegelhub.telemetry.api;

import com.stm.pegelhub.telemetry.domain.Telemetry;
import com.stm.pegelhub.telemetry.application.TelemetryService;
import com.stm.pegelhub.auth.application.AuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * REST controller for telemetry.
 */
@RestController
@RequestMapping("/api/v1/telemetry")
public class HttpTelemetryController {

    private final AuthorizationService authorizationService;
    private final TelemetryService telemetryService;

    public HttpTelemetryController(
            AuthorizationService authorizationService,
            TelemetryService telemetryService) {
        this.authorizationService = requireNonNull(authorizationService);
        this.telemetryService = requireNonNull(telemetryService);
    }

    @Operation(summary = "Adds a new Telemetry Entry to the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns the saved Telemetry Entry",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Telemetry.class))})
    })
    @PostMapping
    public Telemetry writeTelemetryData(
            @RequestParam(name = "apiKey", defaultValue = "") String apiKey,
            @RequestBody Telemetry telemetry) {
        authorizationService.authorize(apiKey);
        return telemetryService.saveTelemetry(telemetry);
    }

    @Operation(summary = "Gets all Telemetry Data in Range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns all Telemetry Data in Range",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Telemetry.class))})
    })
    @GetMapping("/{range}")
    public List<Telemetry> findTelemetryInRange(
            @RequestParam(name = "apiKey", defaultValue = "") String apiKey,
            @PathVariable String range) {
        authorizationService.authorize(apiKey);
        return telemetryService.getByRange(range);
    }

    @Operation(summary = "Gets last Telemetry entry for ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns the telemetry entry",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Telemetry.class))})
    })
    @GetMapping("/last/{uuid}")
    public Telemetry findTelemetryById(
            @RequestParam(name = "apiKey", defaultValue = "") String apiKey,
            @PathVariable UUID uuid) {
        authorizationService.authorize(apiKey);
        return telemetryService.getLastData(uuid);
    }
}
