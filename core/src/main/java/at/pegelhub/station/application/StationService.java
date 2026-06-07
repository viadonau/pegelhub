package at.pegelhub.station.application;

import at.pegelhub.station.domain.Station;
import at.pegelhub.station.domain.StationId;

import java.util.List;

public interface StationService {

    Station create(CreateStationCommand command);

    Station get(StationId id);

    List<Station> list();
}
