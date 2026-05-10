package at.pegelhub.supplier.application;

import at.pegelhub.supplier.domain.Supplier;

import java.util.List;
import java.util.UUID;

/**
 * Service for all {@code Supplier}s.
 */
public interface SupplierService {

    /**
     * Creates a supplier.
     *
     * @param supplier to save.
     * @return the saved supplier.
     */
    Supplier saveSupplier(Supplier supplier);

    /**
     * Get a supplier by its id.
     *
     * @param uuid of the supplier.
     * @return the found supplier.
     */
    Supplier getSupplierById(UUID uuid);

    /**
     * Get all suppliers.
     *
     * @return the found suppliers.
     */
    List<Supplier> getAllSuppliers();

    /**
     * Deletes a supplier by its id.
     *
     * @param uuid of the supplier to delete.
     */
    void deleteSupplier(UUID uuid);

    UUID getConnectorID(UUID uuid);

    Supplier updateSupplier(Supplier supplier);
}
