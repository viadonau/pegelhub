package at.pegelhub.supplier.application;

import at.pegelhub.supplier.domain.StationManufacturer;
import at.pegelhub.supplier.persistence.StationManufacturerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * Default implementation for {@code StationManufacturerService}.
 */
@Service
public final class StationManufacturerServiceImpl implements StationManufacturerService {

    private final StationManufacturerRepository stationManufacturerRepository;

    public StationManufacturerServiceImpl(StationManufacturerRepository stationManufacturerRepository) {
        this.stationManufacturerRepository = requireNonNull(stationManufacturerRepository);
    }

    /**
     * @param stationManufacturer to save.
     * @return the saved {@link StationManufacturer}
     */
    @Override
    public StationManufacturer createStationManufacturer(StationManufacturer stationManufacturer) {
        return stationManufacturerRepository.saveStationManufacturer(stationManufacturer);
    }

    /**
     * @param uuid of the station manufacturer to be searched for.
     * @return the corresponding {@link StationManufacturer} to the specified {@link UUID}
     */
    @Override
    public StationManufacturer getStationManufacturerById(UUID uuid) {
        return stationManufacturerRepository.getById(uuid);
    }

    /**
     * @return all saved {@link StationManufacturer}s
     */
    @Override
    public List<StationManufacturer> getAllStationManufacturers() {
        return stationManufacturerRepository.getAllStationManufacturers();
    }

    /**
     * @param uuid {@link UUID} of the station manufacturer to delete.
     */
    @Override
    public void deleteStationManufacturer(UUID uuid) {
        stationManufacturerRepository.deleteStationManufacturer(uuid);

    }
}
