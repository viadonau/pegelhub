package com.stm.pegelhub.taker.persistence;

import com.stm.pegelhub.testsupport.JpaIntegrationTestBase;
import com.stm.pegelhub.taker.domain.TakerServiceManufacturer;
import com.stm.pegelhub.taker.persistence.JpaTakerServiceManufacturerRepository;
import com.stm.pegelhub.taker.persistence.JdbcTakerServiceManufacturerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final public class TakerServiceManufacturerPostgresIntegrationTest extends JpaIntegrationTestBase {

    private JdbcTakerServiceManufacturerRepository jdbcTakerServiceManufacturerRepository;

    @Autowired
    private JpaTakerServiceManufacturerRepository jpaTakerServiceManufacturerRepository;

    @BeforeEach
    void prepare(){
        jdbcTakerServiceManufacturerRepository = new JdbcTakerServiceManufacturerRepository(jpaTakerServiceManufacturerRepository);
    }

    @Test
    void testSaveTakerServiceManufacturer() {
        TakerServiceManufacturer takerServiceManufacturer = jdbcTakerServiceManufacturerRepository.getById(UUID.fromString("a0be1463-fdf5-4267-84ce-9ddc53bfeb80"))
                .withId(UUID.fromString("e3c26b47-106e-4b4a-8f6d-395f97ee2504"));

        jdbcTakerServiceManufacturerRepository.saveTakerServiceManufacturer(takerServiceManufacturer);
        assertEquals(2, jpaTakerServiceManufacturerRepository.findAll().size());

    }

    @Test
    void testGetById() {
        TakerServiceManufacturer takerServiceManufacturer = jdbcTakerServiceManufacturerRepository.getById(UUID.fromString("a0be1463-fdf5-4267-84ce-9ddc53bfeb80"));
        assertNotNull(takerServiceManufacturer);
    }

    @Test
    void testDeleteTakerServiceManufacturer() {
        TakerServiceManufacturer takerServiceManufacturer = jdbcTakerServiceManufacturerRepository.getById(UUID.fromString("a0be1463-fdf5-4267-84ce-9ddc53bfeb80"));
        jdbcTakerServiceManufacturerRepository.deleteTakerServiceManufacturer(takerServiceManufacturer.getId());

        assertNull(jdbcTakerServiceManufacturerRepository.getById(UUID.fromString("a0be1463-fdf5-4267-84ce-9ddc53bfeb80")));
    }

    @Test
    void testUpdate() {
        TakerServiceManufacturer takerServiceManufacturer = jdbcTakerServiceManufacturerRepository.getById(UUID.fromString("a0be1463-fdf5-4267-84ce-9ddc53bfeb80"));
        takerServiceManufacturer.setStationManufacturerFirmwareVersion("1.1.1");
        jdbcTakerServiceManufacturerRepository.update(takerServiceManufacturer);

        TakerServiceManufacturer updatedTakerServiceManufacturer = jdbcTakerServiceManufacturerRepository.getById(UUID.fromString("a0be1463-fdf5-4267-84ce-9ddc53bfeb80"));

        assertNotNull(updatedTakerServiceManufacturer);
        assertEquals(updatedTakerServiceManufacturer.getStationManufacturerFirmwareVersion(), "1.1.1");
    }

    @Test
    void testGetAllTakerServiceManufacturers() {
        assertEquals(1, jdbcTakerServiceManufacturerRepository.getAllTakerServiceManufacturers().size());
    }
}
