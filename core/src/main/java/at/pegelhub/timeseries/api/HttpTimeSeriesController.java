package at.pegelhub.timeseries.api;

import at.pegelhub.station.domain.StationId;
import at.pegelhub.timeseries.application.TimeSeriesService;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@RestController
@RequestMapping("/api/v1/time-series")
final class HttpTimeSeriesController {

    private final TimeSeriesService timeSeries;

    HttpTimeSeriesController(TimeSeriesService timeSeries) {
        this.timeSeries = requireNonNull(timeSeries);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TimeSeriesResponse create(@Valid @RequestBody CreateTimeSeriesRequest request) {
        return TimeSeriesMapper.toResponse(timeSeries.create(TimeSeriesMapper.toCommand(request)));
    }

    @GetMapping("/{id}")
    TimeSeriesResponse get(@PathVariable UUID id) {
        return TimeSeriesMapper.toResponse(timeSeries.get(new TimeSeriesId(id)));
    }

    @GetMapping
    List<TimeSeriesResponse> list(@RequestParam(required = false) UUID stationId) {
        var result = stationId == null
                ? timeSeries.list()
                : timeSeries.listForStation(new StationId(stationId));
        return result.stream()
                .map(TimeSeriesMapper::toResponse)
                .toList();
    }
}
