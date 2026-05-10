package com.stm.pegelhub.auth.api;

import com.stm.pegelhub.auth.api.ApiTokenDto;
import com.stm.pegelhub.auth.application.ApiTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * REST controller for API token management.
 */
@RestController
@RequestMapping("/api/v1/token")
public class HttpApiTokenController {

    private final ApiTokenService apiTokenService;

    public HttpApiTokenController(ApiTokenService apiTokenService) {
        this.apiTokenService = requireNonNull(apiTokenService);
    }


    @Operation(summary = "Creates a new ApiToken")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns the created ApiToken")
    })
    @PostMapping
    public ApiTokenDto createToken() {
        return new ApiTokenDto(apiTokenService.createToken());
    }

    @Operation(summary = "Refreshes a ApiToken")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns the refreshed ApiToken")
    })
    @PutMapping
    public ApiTokenDto refreshToken(@RequestParam String apiKey, @RequestParam String uuid) {
        return new ApiTokenDto(apiTokenService.refreshToken(apiKey, UUID.fromString(uuid)));
    }

    @Operation(summary = "Invalidates the sent ApiToken")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201")
    })
    @DeleteMapping
    public void invalidateToken(@RequestParam String apiKey, @RequestParam String uuid) {
        apiTokenService.invalidateToken(apiKey, UUID.fromString(uuid));
    }

    @Operation(summary = "Gets the UUIDs of all existing ApiTokens")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns the UUIDs of all existing ApiTokens")
    })
    @GetMapping("/admin")
    public List<UUID> getTokens() {
        return apiTokenService.getTokens();
    }

    @Operation(summary = "Activates an ApiToken by UUID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201")
    })
    @PutMapping("/admin")
    public void activateToken(@RequestParam String uuid) {
        apiTokenService.activateToken(UUID.fromString(uuid));
    }
}
