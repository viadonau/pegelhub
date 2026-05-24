package at.pegelhub.supplier.persistence;

import at.pegelhub.supplier.domain.Supplier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for all {@code Supplier}s.
 */
public interface SupplierRepository {

    /**
     * Saves a supplier to the repository.
     *
     * @param supplier to save.
     * @return the saved supplier.
     */
    Supplier saveSupplier(Supplier supplier);

    /**
     * Get a supplier from the repository by its id.
     *
     * @param uuid of the supplier.
     * @return the found supplier.
     */
    Supplier getById(UUID uuid);

    /**
     * Get all suppliers stored in the repository.
     *
     * @return the found suppliers.
     */
    List<Supplier> getAllSuppliers();

    /**
     * Updates a supplier in the repository.
     *
     * @param supplier to update.
     * @return the updated supplier.
     */
    Supplier update(Supplier supplier);

    /**
     * Deletes a supplier by its id.
     *
     * @param uuid of the supplier to delete.
     */
    void deleteSupplier(UUID uuid);

    /**
     * Returns a supplier, if one already exists for this stationNumber.
     *
     * @param stationNumber the name of the station.
     * @return the supplier for the given name.
     */
    Optional<Supplier> findByStationNumber(String stationNumber);

    Optional<Supplier> findByConnectorKeycloakClientId(String keycloakClientId);

    UUID getConnectorID(UUID uuid);
}
