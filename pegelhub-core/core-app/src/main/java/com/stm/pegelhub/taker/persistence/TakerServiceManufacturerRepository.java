package com.stm.pegelhub.taker.persistence;

import com.stm.pegelhub.taker.domain.TakerServiceManufacturer;

import java.util.List;
import java.util.UUID;

/**
 * Repository for all {@code TakerServiceManufacturer}s.
 */
public interface TakerServiceManufacturerRepository {

    /**
     * Saves a taker service manufacturer to the repository.
     *
     * @param takerServiceManufacturer to save.
     * @return the saved taker service manufacturer.
     */
    TakerServiceManufacturer saveTakerServiceManufacturer(TakerServiceManufacturer takerServiceManufacturer);

    /**
     * Get a taker service manufacturer from the repository by its id.
     *
     * @param uuid of the taker service manufacturer.
     * @return the found taker service manufacturer.
     */
    TakerServiceManufacturer getById(UUID uuid);

    /**
     * Get all taker service manufacturers stored in the repository.
     *
     * @return the found taker service manufacturers.
     */
    List<TakerServiceManufacturer> getAllTakerServiceManufacturers();

    /**
     * Updates a taker service manufacturer in the repository.
     *
     * @param takerServiceManufacturer to update.
     * @return the updated taker service manufacturer.
     */
    TakerServiceManufacturer update(TakerServiceManufacturer takerServiceManufacturer);

    /**
     * Deletes a taker service manufacturer by its id.
     *
     * @param uuid of the taker service manufacturer to delete.
     */
    void deleteTakerServiceManufacturer(UUID uuid);
}

