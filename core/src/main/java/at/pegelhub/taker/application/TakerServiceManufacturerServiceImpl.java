package at.pegelhub.taker.application;

import at.pegelhub.taker.domain.TakerServiceManufacturer;
import at.pegelhub.taker.persistence.TakerServiceManufacturerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * Default implementation for {@code TakerServiceManufacturerService}.
 */
@Service
public final class TakerServiceManufacturerServiceImpl implements TakerServiceManufacturerService {

    private final TakerServiceManufacturerRepository takerServiceManufacturerRepository;

    public TakerServiceManufacturerServiceImpl(TakerServiceManufacturerRepository takerServiceManufacturerRepository) {
        this.takerServiceManufacturerRepository = requireNonNull(takerServiceManufacturerRepository);
    }

    /**
     * @param takerServiceManufacturer to save.
     * @return the saved {@link TakerServiceManufacturer}
     */
    @Override
    public TakerServiceManufacturer createTakerServiceManufacturer(TakerServiceManufacturer takerServiceManufacturer) {
        return takerServiceManufacturerRepository.saveTakerServiceManufacturer(takerServiceManufacturer);
    }

    /**
     * @param uuid of the taker service manufacturer to be searched for
     * @return the corresponding {@link TakerServiceManufacturer} to the specified {@link UUID}
     */
    @Override
    public TakerServiceManufacturer getTakerServiceManufacturerById(UUID uuid) {
        return takerServiceManufacturerRepository.getById(uuid);
    }

    /**
     * @return all saved {@link TakerServiceManufacturer}s
     */
    @Override
    public List<TakerServiceManufacturer> getAllTakerServiceManufacturers() {
        return takerServiceManufacturerRepository.getAllTakerServiceManufacturers();
    }

    /**
     * @param uuid {@link UUID} of the taker service manufacturer {@link TakerServiceManufacturer} to delete.
     */
    @Override
    public void deleteTakerServiceManufacturer(UUID uuid) {
        takerServiceManufacturerRepository.deleteTakerServiceManufacturer(uuid);

    }
}
