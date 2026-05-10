package com.stm.pegelhub.taker.application;

import com.stm.pegelhub.taker.domain.Taker;

import java.util.List;
import java.util.UUID;

/**
 * Service for all {@code Taker}s.
 */
public interface TakerService {

    /**
     * Creates a taker.
     *
     * @param taker to save.
     * @return the saved taker.
     */
    Taker saveTaker(Taker taker);

    /**
     * Get a taker by its id.
     *
     * @param uuid of the taker.
     * @return the found taker.
     */
    Taker getTakerById(UUID uuid);

    /**
     * Get all takers.
     *
     * @return the found takers.
     */
    List<Taker> getAllTakers();

    /**
     * Deletes a taker by its id.
     *
     * @param uuid of the taker to delete.
     */
    void deleteTaker(UUID uuid);
}
