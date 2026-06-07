package at.pegelhub.station.application;

import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.station.domain.Station;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.station.persistence.StationRepository;
import at.pegelhub.stationowner.application.StationOwnerService;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.Objects.requireNonNull;

@Service
final class StationServiceImpl implements StationService {

    private final StationRepository stations;
    private final StationOwnerService stationOwners;

    StationServiceImpl(StationRepository stations, StationOwnerService stationOwners) {
        this.stations = requireNonNull(stations);
        this.stationOwners = requireNonNull(stationOwners);
    }

    @Override
    public Station create(CreateStationCommand command) {
        requireNonNull(command);
        stationOwners.get(command.ownerId());
        return stations.save(Station.create(
                command.ownerId(),
                command.stationNumber(),
                command.name(),
                command.waterBody(),
                command.location()));
    }

    @Override
    public Station get(StationId id) {
        requireNonNull(id);
        return stations.findById(id)
                .orElseThrow(() -> new NotFoundException("Station not found: " + id.value()));
    }

    @Override
    public List<Station> list() {
        return stations.findAll();
    }
}
