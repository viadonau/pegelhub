package com.stm.pegelhub.supplier.persistence;

import com.stm.pegelhub.testsupport.JpaIntegrationTestBase;
import com.stm.pegelhub.supplier.domain.Supplier;
import com.stm.pegelhub.supplier.persistence.JpaSupplierRepository;
import com.stm.pegelhub.supplier.persistence.JdbcSupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final public class SupplierPostgresIntegrationTest extends JpaIntegrationTestBase {

    private JdbcSupplierRepository jdbcSupplierRepository;

    @Autowired
    private JpaSupplierRepository jpaSupplierRepository;

    @BeforeEach
    void prepare() {
        jdbcSupplierRepository = new JdbcSupplierRepository(jpaSupplierRepository);
    }

    @Test
    void testSaveSupplier() {
        Supplier supplier = jdbcSupplierRepository.getById(UUID.fromString("f68c2af4-86b1-46df-a4f0-b0450d4b6a8c"))
                .withId(UUID.fromString("80503307-329e-49b7-bc33-144c0e17aabe"));

        jdbcSupplierRepository.saveSupplier(supplier);
        assertEquals(2, jpaSupplierRepository.findAll().size());

    }

    @Test
    void testGetById() {
        Supplier supplier = jdbcSupplierRepository.getById(UUID.fromString("f68c2af4-86b1-46df-a4f0-b0450d4b6a8c"));
        assertNotNull(supplier);
    }

    @Test
    void testDeleteSupplier() {
        Supplier supplier = jdbcSupplierRepository.getById(UUID.fromString("f68c2af4-86b1-46df-a4f0-b0450d4b6a8c"));
        jdbcSupplierRepository.deleteSupplier(supplier.getId());

        assertNull(jdbcSupplierRepository.getById(UUID.fromString("f68c2af4-86b1-46df-a4f0-b0450d4b6a8c")));
    }

    @Test
    void testUpdate() {
        Supplier supplier = jdbcSupplierRepository.getById(UUID.fromString("f68c2af4-86b1-46df-a4f0-b0450d4b6a8c"));
        supplier.setAccuracy(2.5);
        jdbcSupplierRepository.update(supplier);

        Supplier updatedSupplier = jdbcSupplierRepository.getById(UUID.fromString("f68c2af4-86b1-46df-a4f0-b0450d4b6a8c"));

        assertNotNull(updatedSupplier);
        assertEquals(updatedSupplier.getAccuracy(), 2.5);
    }

    @Test
    void testGetAllSuppliers() {
        assertEquals(1, jdbcSupplierRepository.getAllSuppliers().size());
    }
}
