package com.stm.pegelhub.supplier.application;

import com.stm.pegelhub.supplier.domain.Supplier;
import com.stm.pegelhub.connector.persistence.ConnectorRepository;
import com.stm.pegelhub.contact.persistence.ContactRepository;
import com.stm.pegelhub.supplier.persistence.StationManufacturerRepository;
import com.stm.pegelhub.supplier.persistence.SupplierRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static com.stm.pegelhub.testsupport.ExampleData.SUPPLIER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

final class SupplierServiceImplTest {

    private SupplierServiceImpl supplierService;
    private static final SupplierRepository REPOSITORY = mock(SupplierRepository.class);
    private static final StationManufacturerRepository STATION_MANUFACTURER_REPOSITORY = mock(StationManufacturerRepository.class);
    private static final ConnectorRepository CONNECTOR_REPOSITORY = mock(ConnectorRepository.class);
    private static final ContactRepository CONTACT_REPOSITORY = mock(ContactRepository.class);

    @BeforeEach
    public void prepare() {
        supplierService = new SupplierServiceImpl(REPOSITORY, STATION_MANUFACTURER_REPOSITORY, CONNECTOR_REPOSITORY,
                CONTACT_REPOSITORY);
        reset(REPOSITORY, STATION_MANUFACTURER_REPOSITORY, CONNECTOR_REPOSITORY, CONTACT_REPOSITORY);
        when(STATION_MANUFACTURER_REPOSITORY.saveStationManufacturer(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(CONNECTOR_REPOSITORY.saveConnector(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(CONTACT_REPOSITORY.saveContact(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    public void constructorWithNullArgsThrowsNPE() {
        assertThrows(NullPointerException.class, () -> new SupplierServiceImpl(null, STATION_MANUFACTURER_REPOSITORY, CONNECTOR_REPOSITORY, CONTACT_REPOSITORY));
        assertThrows(NullPointerException.class, () -> new SupplierServiceImpl(REPOSITORY, null, CONNECTOR_REPOSITORY, CONTACT_REPOSITORY));
        assertThrows(NullPointerException.class, () -> new SupplierServiceImpl(REPOSITORY, STATION_MANUFACTURER_REPOSITORY, null, CONTACT_REPOSITORY));
        assertThrows(NullPointerException.class, () -> new SupplierServiceImpl(REPOSITORY, STATION_MANUFACTURER_REPOSITORY, CONNECTOR_REPOSITORY, null));
    }

    @Test
    public void createSupplier() {
        when(REPOSITORY.saveSupplier(any())).thenReturn(SUPPLIER);

        Supplier result = supplierService.saveSupplier(SUPPLIER);
        assertEquals(SUPPLIER, result);
        verify(REPOSITORY, times(1)).saveSupplier(any());
    }

    @Test
    public void getById() {
        when(REPOSITORY.getById(any())).thenReturn(SUPPLIER);

        Supplier result = supplierService.getSupplierById(UUID.randomUUID());
        assertEquals(SUPPLIER, result);
        verify(REPOSITORY, times(1)).getById(any());
    }


    @Test
    public void getAll() {
        when(REPOSITORY.getAllSuppliers()).thenReturn(List.of(SUPPLIER));

        List<Supplier> result = supplierService.getAllSuppliers();
        assertEquals(1, result.size());
        Assertions.assertThat(result).containsOnly(SUPPLIER);
        verify(REPOSITORY, times(1)).getAllSuppliers();
    }
}
