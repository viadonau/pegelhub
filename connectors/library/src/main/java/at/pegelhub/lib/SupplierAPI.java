package at.pegelhub.lib;

import at.pegelhub.lib.model.Supplier;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface SupplierAPI {

    Collection<Supplier> getSuppliers();

    Optional<Supplier> getSupplierbyUUID(UUID uuid);

    UUID getConnectorID(UUID uuid);
}
