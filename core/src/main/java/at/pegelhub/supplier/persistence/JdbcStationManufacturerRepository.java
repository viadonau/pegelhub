package at.pegelhub.supplier.persistence;

import at.pegelhub.shared.persistence.DomainToJpaConverter;
import at.pegelhub.shared.persistence.JpaToDomainConverter;
import at.pegelhub.shared.persistence.*;

import at.pegelhub.supplier.domain.StationManufacturer;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * JDBC Implementation of the Interface {@code StationManufacturerRepository}.
 */
@Repository
public class JdbcStationManufacturerRepository implements StationManufacturerRepository {
    private final JpaStationManufacturerRepository jpaStationManufacturerRepository;

    public JdbcStationManufacturerRepository(JpaStationManufacturerRepository jpaStationManufacturerRepository) {
        this.jpaStationManufacturerRepository = jpaStationManufacturerRepository;
    }

    /**
     * @param stationManufacturer to save.
     * @return the saved {@link StationManufacturer}
     */
    @Override
    public StationManufacturer saveStationManufacturer(StationManufacturer stationManufacturer) {
        if (stationManufacturer.getId() == null) {
            stationManufacturer = stationManufacturer.withId(UUID.randomUUID());
        }
        return JpaToDomainConverter.convert(jpaStationManufacturerRepository.save(DomainToJpaConverter.convert(stationManufacturer)));
    }

    /**
     * @param uuid of the station manufacturer.
     * @return the corresponding {@link StationManufacturer} to the given {@link UUID}
     */
    @Override
    public StationManufacturer getById(UUID uuid) {
        return jpaStationManufacturerRepository.findById(uuid).map(JpaToDomainConverter::convert).orElse(null);
    }

    /**
     * @return all saved {@link StationManufacturer}
     */
    @Override
    public List<StationManufacturer> getAllStationManufacturers() {
        return JpaToDomainConverter.convert(jpaStationManufacturerRepository.findAll());
    }

    /**
     * @param stationManufacturer to update.
     * @return the updated {@link StationManufacturer}
     */
    @Override
    public StationManufacturer update(StationManufacturer stationManufacturer) {
        return JpaToDomainConverter.convert(jpaStationManufacturerRepository.save(DomainToJpaConverter.convert(stationManufacturer)));
    }

    /**
     * @param uuid of the {@link StationManufacturer} to delete.
     */
    @Override
    public void deleteStationManufacturer(UUID uuid) {
        jpaStationManufacturerRepository.delete(jpaStationManufacturerRepository.findById(uuid).orElseThrow());
    }
}
