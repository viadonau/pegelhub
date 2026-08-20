package at.pegelhub.access.api;

import at.pegelhub.access.application.ConnectorReadAccessService;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

@RestController
@RequestMapping("/api/v1/connectors/{connectorId}/read-access")
@Tag(
        name = "openapi.access.http-connector-read-access-controller.connector-read-access",
        description = "openapi.access.http-connector-read-access-controller.manage-explicit-read-access-relations")
@SecurityRequirement(name = "bearerAuth")
public final class HttpConnectorReadAccessController {
    private final ConnectorReadAccessService access;

    HttpConnectorReadAccessController(ConnectorReadAccessService access) { this.access = requireNonNull(access); }

    @PutMapping("/stations/{stationId}")
    @Operation(
            operationId = "grantConnectorStationReadAccess",
            summary = "openapi.access.http-connector-read-access-controller.grant-station-read-access")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "openapi.access.http-connector-read-access-controller.read-access-granted"),
            @ApiResponse(
                    responseCode = "404",
                    description = "openapi.access.http-connector-read-access-controller.connector-or-station-not-found",
                    content = @Content)
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void grantStation(@PathVariable UUID connectorId, @PathVariable UUID stationId) {
        access.grantStation(new ConnectorId(connectorId), new StationId(stationId));
    }

    @DeleteMapping("/stations/{stationId}")
    @Operation(
            operationId = "revokeConnectorStationReadAccess",
            summary = "openapi.access.http-connector-read-access-controller.revoke-station-read-access")
    @ApiResponse(
            responseCode = "204",
            description = "openapi.access.http-connector-read-access-controller.read-access-revoked")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeStation(@PathVariable UUID connectorId, @PathVariable UUID stationId) {
        access.revokeStation(new ConnectorId(connectorId), new StationId(stationId));
    }

    @PutMapping("/time-series/{timeSeriesId}")
    @Operation(
            operationId = "grantConnectorTimeSeriesReadAccess",
            summary = "openapi.access.http-connector-read-access-controller.grant-time-series-read-access")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "openapi.access.http-connector-read-access-controller.read-access-granted"),
            @ApiResponse(
                    responseCode = "404",
                    description = "openapi.access.http-connector-read-access-controller.connector-or-time-series-not-found",
                    content = @Content)
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void grantTimeSeries(@PathVariable UUID connectorId, @PathVariable UUID timeSeriesId) {
        access.grantTimeSeries(new ConnectorId(connectorId), new TimeSeriesId(timeSeriesId));
    }

    @DeleteMapping("/time-series/{timeSeriesId}")
    @Operation(
            operationId = "revokeConnectorTimeSeriesReadAccess",
            summary = "openapi.access.http-connector-read-access-controller.revoke-time-series-read-access")
    @ApiResponse(
            responseCode = "204",
            description = "openapi.access.http-connector-read-access-controller.read-access-revoked")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeTimeSeries(@PathVariable UUID connectorId, @PathVariable UUID timeSeriesId) {
        access.revokeTimeSeries(new ConnectorId(connectorId), new TimeSeriesId(timeSeriesId));
    }
}
