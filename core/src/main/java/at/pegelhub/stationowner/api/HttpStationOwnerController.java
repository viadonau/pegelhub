package at.pegelhub.stationowner.api;

import at.pegelhub.stationowner.application.StationOwnerService;
import at.pegelhub.stationowner.domain.StationOwnerId;
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
final class HttpStationOwnerController {

    private final StationOwnerService stationOwners;

    HttpStationOwnerController(StationOwnerService stationOwners) {
        this.stationOwners = requireNonNull(stationOwners);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    StationOwnerResponse create(@Valid @RequestBody CreateStationOwnerRequest request) {
        return StationOwnerMapper.toResponse(stationOwners.create(StationOwnerMapper.toCommand(request)));
    }

    @GetMapping("/{id}")
    StationOwnerResponse get(@PathVariable UUID id) {
        return StationOwnerMapper.toResponse(stationOwners.get(new StationOwnerId(id)));
    }

    @GetMapping
    List<StationOwnerResponse> list() {
        return stationOwners.list().stream()
                .map(StationOwnerMapper::toResponse)
                .toList();
    }
}
