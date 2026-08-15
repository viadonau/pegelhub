package at.pegelhub.monitoring.api;

import at.pegelhub.monitoring.application.MonitoringQueryService;
import at.pegelhub.shared.duration.PegelhubDurationLiteral;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@RestController
@RequestMapping("/api/v1/monitoring/time-series")
@Tag(name = "Monitoring", description = "openapi.monitoring.monitoring-controller.operator-monitoring-read-views")
@SecurityRequirement(name = "bearerAuth")
final class MonitoringController {

    private final MonitoringQueryService monitoring;

    MonitoringController(MonitoringQueryService monitoring) {
        this.monitoring = requireNonNull(monitoring);
    }

    @Operation(operationId = "getMonitoringTimeSeries", summary = "openapi.monitoring.monitoring-controller.reads-the-operator-monitoring-catalog")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "openapi.monitoring.monitoring-controller.returns-the-monitoring-collection", content = @Content(schema = @Schema(implementation = MonitoringResponse.TimeSeriesCollection.class))),
            @ApiResponse(responseCode = "400", description = "openapi.monitoring.monitoring-controller.latest-window-is-invalid", content = @Content),
            @ApiResponse(responseCode = "500", description = "openapi.monitoring.monitoring-controller.monitoring-metadata-is-inconsistent", content = @Content),
            @ApiResponse(responseCode = "503", description = "openapi.monitoring.monitoring-controller.measurement-store-is-unavailable", content = @Content)
    })
    @GetMapping
    MonitoringResponse.TimeSeriesCollection list(
            @Parameter(description = "openapi.monitoring.monitoring-controller.latest-measurement-window-up-to-365-days", example = "365d")
            @RequestParam(defaultValue = "365d") String latestWithin) {
        return MonitoringResponseMapper.toResponse(monitoring.readCollection(latestWithin(latestWithin)));
    }

    @Operation(operationId = "getMonitoringTimeSeriesDetail", summary = "openapi.monitoring.monitoring-controller.reads-one-operator-monitoring-snapshot")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "openapi.monitoring.monitoring-controller.returns-the-monitoring-detail", content = @Content(schema = @Schema(implementation = MonitoringResponse.TimeSeriesDetail.class))),
            @ApiResponse(responseCode = "400", description = "openapi.monitoring.monitoring-controller.latest-window-is-invalid", content = @Content),
            @ApiResponse(responseCode = "404", description = "openapi.monitoring.monitoring-controller.time-series-was-not-found", content = @Content),
            @ApiResponse(responseCode = "500", description = "openapi.monitoring.monitoring-controller.monitoring-metadata-is-inconsistent", content = @Content),
            @ApiResponse(responseCode = "503", description = "openapi.monitoring.monitoring-controller.measurement-store-is-unavailable", content = @Content)
    })
    @GetMapping("/{timeSeriesId}")
    MonitoringResponse.TimeSeriesDetail get(
            @PathVariable UUID timeSeriesId,
            @Parameter(description = "openapi.monitoring.monitoring-controller.latest-measurement-window-up-to-365-days", example = "365d")
            @RequestParam(defaultValue = "365d") String latestWithin) {
        return MonitoringResponseMapper.toResponse(monitoring.readDetail(
                new TimeSeriesId(timeSeriesId), latestWithin(latestWithin)));
    }

    private static Duration latestWithin(String value) {
        Duration duration;
        try {
            duration = new PegelhubDurationLiteral(value).toDuration();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("latestWithin is too large", exception);
        }
        if (duration.compareTo(Duration.ofDays(365)) > 0) {
            throw new IllegalArgumentException("latestWithin must not exceed 365d");
        }
        return duration;
    }
}
