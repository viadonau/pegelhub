package at.pegelhub.station.persistence;

import at.pegelhub.station.domain.Station;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.stationowner.domain.StationOwnerId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Repository
final class JpaStationRepositoryAdapter implements StationRepository {

    private final SpringDataStationRepository stations;

    JpaStationRepositoryAdapter(SpringDataStationRepository stations) {
        this.stations = requireNonNull(stations);
    }

    @Override
    public Station save(Station station) {
        requireNonNull(station);
        return toDomain(stations.save(toJpa(station)));
    }

    @Override
    public Optional<Station> findById(StationId id) {
        requireNonNull(id);
        return stations.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<Station> findAll() {
        return stations.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    private JpaStation toJpa(Station station) {
        return new JpaStation(
                station.id().value(),
                station.ownerId().value(),
                station.stationNumber(),
                station.name(),
                station.waterBody(),
                station.location());
    }

    private Station toDomain(JpaStation station) {
        return new Station(
                new StationId(station.id()),
                new StationOwnerId(station.ownerId()),
                station.stationNumber(),
                station.name(),
                station.waterBody(),
                station.location());
    }
}
