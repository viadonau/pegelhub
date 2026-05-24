package at.pegelhub.taker.persistence;

import at.pegelhub.taker.domain.Taker;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for all {@code Taker}s.
 */
public interface TakerRepository {

    /**
     * Saves a taker to the repository.
     *
     * @param taker to save.
     * @return the saved taker.
     */
    Taker saveTaker(Taker taker);

    /**
     * Get a taker from the repository by its id.
     *
     * @param uuid of the taker.
     * @return the found taker.
     */
    Taker getById(UUID uuid);

    /**
     * Get all takers stored in the repository.
     *
     * @return the found takers.
     */
    List<Taker> getAllTakers();

    /**
     * Updates a taker in the repository.
     *
     * @param taker to update.
     * @return the updated taker.
     */
    Taker update(Taker taker);

    /**
     * Deletes a taker by its id.
     *
     * @param uuid of the taker to delete.
     */
    void deleteTaker(UUID uuid);

    /**
     * Returns a taker, if one already exists for this stationNumber.
     *
     * @param stationNumber the name of the station.
     * @return the taker for the given name.
     */
    Optional<Taker> findByStationNumber(String stationNumber);

    /**
     * Returns the taker bound to the given Keycloak client id.
     *
     * @param keycloakClientId the connector identity from the JWT.
     * @return the taker for the given connector identity.
     */
    Optional<Taker> findByConnectorKeycloakClientId(String keycloakClientId);
}
