package at.pegelhub.station.api;

import at.pegelhub.station.application.StationService;
import at.pegelhub.station.domain.StationId;
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
final class HttpStationController {

    private final StationService stations;

    HttpStationController(StationService stations) {
        this.stations = requireNonNull(stations);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    StationResponse create(@Valid @RequestBody CreateStationRequest request) {
        return StationMapper.toResponse(stations.create(StationMapper.toCommand(request)));
    }

    @GetMapping("/{id}")
    StationResponse get(@PathVariable UUID id) {
        return StationMapper.toResponse(stations.get(new StationId(id)));
    }

    @GetMapping
    List<StationResponse> list() {
        return stations.list().stream()
                .map(StationMapper::toResponse)
                .toList();
    }
}
