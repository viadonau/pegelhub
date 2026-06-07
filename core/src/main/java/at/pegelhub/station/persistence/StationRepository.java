package at.pegelhub.station.persistence;

import at.pegelhub.station.domain.Station;
import at.pegelhub.station.domain.StationId;

import java.util.List;
import java.util.Optional;

public interface StationRepository {

    Station save(Station station);

    Optional<Station> findById(StationId id);

    List<Station> findAll();
}
