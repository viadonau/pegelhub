package at.pegelhub.supplier.application;

import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.persistence.ConnectorRepository;
import at.pegelhub.contact.persistence.ContactRepository;
import at.pegelhub.supplier.domain.Supplier;
import at.pegelhub.supplier.persistence.StationManufacturerRepository;
import at.pegelhub.supplier.persistence.SupplierRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static at.pegelhub.testsupport.ExampleData.SUPPLIER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class SupplierServiceImplTest {

    private SupplierServiceImpl supplierService;

    private static final SupplierRepository REPOSITORY = mock(SupplierRepository.class);
    private static final StationManufacturerRepository STATION_MANUFACTURER_REPOSITORY = mock(StationManufacturerRepository.class);
    private static final ConnectorRepository CONNECTOR_REPOSITORY = mock(ConnectorRepository.class);
    private static final ContactRepository CONTACT_REPOSITORY = mock(ContactRepository.class);

    @BeforeEach
    void prepare() {
        supplierService = new SupplierServiceImpl(REPOSITORY, STATION_MANUFACTURER_REPOSITORY, CONNECTOR_REPOSITORY,
                CONTACT_REPOSITORY);

        reset(REPOSITORY, STATION_MANUFACTURER_REPOSITORY, CONNECTOR_REPOSITORY, CONTACT_REPOSITORY);
        when(STATION_MANUFACTURER_REPOSITORY.saveStationManufacturer(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(CONNECTOR_REPOSITORY.saveConnector(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(CONTACT_REPOSITORY.saveContact(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void constructorWithNullArgsThrowsNpe() {
        assertThrows(NullPointerException.class, () -> new SupplierServiceImpl(null, STATION_MANUFACTURER_REPOSITORY, CONNECTOR_REPOSITORY, CONTACT_REPOSITORY));
        assertThrows(NullPointerException.class, () -> new SupplierServiceImpl(REPOSITORY, null, CONNECTOR_REPOSITORY, CONTACT_REPOSITORY));
        assertThrows(NullPointerException.class, () -> new SupplierServiceImpl(REPOSITORY, STATION_MANUFACTURER_REPOSITORY, null, CONTACT_REPOSITORY));
        assertThrows(NullPointerException.class, () -> new SupplierServiceImpl(REPOSITORY, STATION_MANUFACTURER_REPOSITORY, CONNECTOR_REPOSITORY, null));
    }

    @Test
    void saveSupplierPersistsGraphAndReturnsSupplier() {
        when(REPOSITORY.saveSupplier(any())).thenReturn(SUPPLIER);

        Supplier result = supplierService.saveSupplier(SUPPLIER);

        assertEquals(SUPPLIER, result);
        verify(REPOSITORY).saveSupplier(any());
    }

    @Test
    void saveSupplierReusesExistingSupplierIdentityForMatchingStationNumber() {
        Supplier existing = SUPPLIER.withId(UUID.randomUUID());
        existing.setStationManufacturer(existing.getStationManufacturer().withId(UUID.randomUUID()));
        when(REPOSITORY.findByStationNumber(SUPPLIER.getStationNumber())).thenReturn(Optional.of(existing));
        when(CONNECTOR_REPOSITORY.findByConnectorNumber(SUPPLIER.getConnector().getConnectorNumber()))
                .thenReturn(Optional.of(new Connector()));
        when(REPOSITORY.saveSupplier(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Supplier result = supplierService.saveSupplier(SUPPLIER);

        assertEquals(existing.getId(), result.getId());
        assertEquals(existing.getStationManufacturer().getId(), result.getStationManufacturer().getId());
    }

    @Test
    void updateSupplierDelegatesToSaveBehavior() {
        when(REPOSITORY.saveSupplier(any())).thenReturn(SUPPLIER);

        Supplier result = supplierService.updateSupplier(SUPPLIER);

        assertEquals(SUPPLIER, result);
        verify(REPOSITORY, times(1)).saveSupplier(any());
    }

    @Test
    void getSupplierByIdDelegatesToRepository() {
        when(REPOSITORY.getById(any())).thenReturn(SUPPLIER);

        Supplier result = supplierService.getSupplierById(UUID.randomUUID());

        assertEquals(SUPPLIER, result);
        verify(REPOSITORY).getById(any());
    }

    @Test
    void getAllSuppliersReturnsRepositoryValues() {
        when(REPOSITORY.getAllSuppliers()).thenReturn(List.of(SUPPLIER));

        List<Supplier> result = supplierService.getAllSuppliers();

        assertEquals(1, result.size());
        Assertions.assertThat(result).containsOnly(SUPPLIER);
        verify(REPOSITORY).getAllSuppliers();
    }

    @Test
    void getConnectorIdDelegatesToRepository() {
        UUID supplierId = UUID.randomUUID();
        UUID connectorId = UUID.randomUUID();
        when(REPOSITORY.getConnectorID(supplierId)).thenReturn(connectorId);

        UUID result = supplierService.getConnectorID(supplierId);

        assertEquals(connectorId, result);
        verify(REPOSITORY).getConnectorID(supplierId);
    }
}
