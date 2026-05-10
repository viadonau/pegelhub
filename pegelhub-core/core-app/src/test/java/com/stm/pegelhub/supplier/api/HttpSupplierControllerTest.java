package com.stm.pegelhub.supplier.api;

import com.stm.pegelhub.shared.web.DomainToDtoConverter;

import com.stm.pegelhub.supplier.domain.Supplier;
import com.stm.pegelhub.supplier.api.SupplierDto;
import com.stm.pegelhub.auth.application.AuthorizationService;
import com.stm.pegelhub.supplier.application.SupplierService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.stm.pegelhub.testsupport.ExampleData.SUPPLIER;
import static com.stm.pegelhub.testsupport.ExampleDtos.CREATE_SUPPLIER_DTO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HttpSupplierControllerTest {

    private HttpSupplierController sut;

    private static final AuthorizationService AUTHORIZATION_SERVICE = mock(AuthorizationService.class);
    private static final SupplierService SERVICE = mock(SupplierService.class);

    @BeforeEach
    void setUp() {
        sut = new HttpSupplierController(AUTHORIZATION_SERVICE, SERVICE);
        reset(AUTHORIZATION_SERVICE);
        when(AUTHORIZATION_SERVICE.authorize(anyString())).thenReturn(UUID.randomUUID());
        reset(SERVICE);
    }

    @Test
    public void constructorShouldThrowNullPointerExceptionIfApiTokenServiceIsNull() {
        assertThrows(NullPointerException.class, () -> new HttpSupplierController(null, SERVICE));
        assertThrows(NullPointerException.class, () -> new HttpSupplierController(AUTHORIZATION_SERVICE, null));
    }

    @Test
    void createSupplier() {
        when(SERVICE.saveSupplier(any())).thenReturn(SUPPLIER);
        SupplierDto expected = DomainToDtoConverter.convert(SUPPLIER);
        SupplierDto actual = sut.saveSupplier("", CREATE_SUPPLIER_DTO);
        assertEquals(expected, actual);
        verify(AUTHORIZATION_SERVICE).authorize("");
    }

    @Test
    void getSupplierById() {
        UUID uuid = UUID.randomUUID();
        when(SERVICE.getSupplierById(uuid)).thenReturn(SUPPLIER);
        SupplierDto expected = DomainToDtoConverter.convert(SUPPLIER);
        SupplierDto actual = sut.getSupplierById(uuid);
        assertEquals(expected, actual);
    }

    @Test
    void getAllSuppliers() {
        List<Supplier> suppliers = new ArrayList<>();
        suppliers.add(SUPPLIER);
        when(SERVICE.getAllSuppliers()).thenReturn(suppliers);
        List<SupplierDto> expected = DomainToDtoConverter.convert(suppliers);
        List<SupplierDto> actual = sut.getAllSuppliers();
        assertEquals(expected, actual);
    }

    @Test
    void deleteSupplier() {
        UUID uuid = UUID.randomUUID();
        sut.deleteSupplier(uuid);
        verify(SERVICE, times(1)).deleteSupplier(uuid);
    }
}
