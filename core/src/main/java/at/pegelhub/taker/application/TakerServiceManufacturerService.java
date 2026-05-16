package at.pegelhub.taker.application;

import at.pegelhub.taker.domain.TakerServiceManufacturer;

import java.util.List;
import java.util.UUID;

/**
 * Service for all {@code TakerServiceManufacturer}s.
 */
public interface TakerServiceManufacturerService {

    /**
     * Creates a taker service manufacturer.
     *
     * @param takerServiceManufacturer to save.
     * @return the saved taker service manufacturer.
     */
    TakerServiceManufacturer createTakerServiceManufacturer(TakerServiceManufacturer takerServiceManufacturer);

    /**
     * Get a taker service manufacturer by its id.
     *
     * @param uuid of the taker service manufacturer.
     * @return the found taker service manufacturer.
     */
    TakerServiceManufacturer getTakerServiceManufacturerById(UUID uuid);

    /**
     * Get all taker service manufacturers.
     *
     * @return the found taker service manufacturers.
     */
    List<TakerServiceManufacturer> getAllTakerServiceManufacturers();

    /**
     * Deletes a taker service manufacturer by its id.
     *
     * @param uuid of the taker service manufacturer to delete.
     */
    void deleteTakerServiceManufacturer(UUID uuid);
}
