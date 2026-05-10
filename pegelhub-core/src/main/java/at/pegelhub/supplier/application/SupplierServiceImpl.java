package at.pegelhub.supplier.application;

import at.pegelhub.connector.domain.Connector;
import at.pegelhub.supplier.domain.Supplier;
import at.pegelhub.connector.persistence.ConnectorRepository;
import at.pegelhub.contact.persistence.ContactRepository;
import at.pegelhub.supplier.persistence.StationManufacturerRepository;
import at.pegelhub.supplier.persistence.SupplierRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static at.pegelhub.connector.application.ContactUtil.updateConnector;
import static java.util.Objects.requireNonNull;

/**
 * Default implementation for {@code SupplierService}.
 */
@Service
public final class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final StationManufacturerRepository stationManufacturerRepository;
    private final ConnectorRepository connectorRepository;
    private final ContactRepository contactRepository;


    public SupplierServiceImpl(SupplierRepository supplierRepository,
                               StationManufacturerRepository stationManufacturerRepository,
                               ConnectorRepository connectorRepository,
                               ContactRepository contactRepository) {
        this.supplierRepository = requireNonNull(supplierRepository);
        this.stationManufacturerRepository = requireNonNull(stationManufacturerRepository);
        this.connectorRepository = requireNonNull(connectorRepository);
        this.contactRepository = requireNonNull(contactRepository);
    }

    //TODO: maybe rename to "createSupplier" to align this method to the nomenclature of most of the other Service Implementations

    /**
     * @param supplier to save.
     * @return the saved {@link Supplier}
     */
    @Override
    public Supplier saveSupplier(Supplier supplier) {
        Optional<Supplier> existingSupplier = supplierRepository.findByStationNumber(supplier.getStationNumber());
        Optional<Connector> existingConnector = connectorRepository.findByConnectorNumber(
                supplier.getConnector().getConnectorNumber());
        if (existingSupplier.isPresent()) {
            supplier = supplier.withId(existingSupplier.get().getId());
            supplier.setStationManufacturer(
                    supplier.getStationManufacturer().withId(existingSupplier.get().getStationManufacturer().getId()));
        }
        Connector connector = updateConnector(contactRepository, existingConnector.orElse(null), supplier.getConnector());
        supplier.setStationManufacturer(
                stationManufacturerRepository.saveStationManufacturer(supplier.getStationManufacturer()));
        supplier.setConnector(
                connectorRepository.saveConnector(connector));
        return supplierRepository.saveSupplier(supplier);
    }

    /**
     * @param uuid of the supplier to be searched for
     * @return the corresponding {@link Supplier} to the specified {@link UUID}
     */
    @Override
    public Supplier getSupplierById(UUID uuid) {
        return supplierRepository.getById(uuid);
    }

    /**
     * @return all saved {@link Supplier}s
     */
    @Override
    public List<Supplier> getAllSuppliers() {
        return supplierRepository.getAllSuppliers();
    }

    /**
     * @param uuid {@link UUID} of the supplier to delete.
     */
    @Override
    public void deleteSupplier(UUID uuid) {
        supplierRepository.deleteSupplier(uuid);

    }

    @Override
    public UUID getConnectorID(UUID uuid) {
        return supplierRepository.getConnectorID(uuid);
    }

    @Override
    public Supplier updateSupplier(Supplier supplier) {
        return saveSupplier(supplier);
    }
}
