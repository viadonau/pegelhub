package com.stm.pegelhub.supplier.persistence;

import com.stm.pegelhub.testsupport.JpaIntegrationTestBase;
import com.stm.pegelhub.supplier.domain.StationManufacturer;
import com.stm.pegelhub.supplier.persistence.JpaStationManufacturerRepository;
import com.stm.pegelhub.supplier.persistence.JdbcStationManufacturerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final public class StationManufacturerPostgresIntegrationTest extends JpaIntegrationTestBase {

    private JdbcStationManufacturerRepository jdbcStationManufacturerRepository;

    @Autowired
    private JpaStationManufacturerRepository jpaStationManufacturerRepository;

    @BeforeEach
    void prepare(){
        jdbcStationManufacturerRepository = new JdbcStationManufacturerRepository(jpaStationManufacturerRepository);
    }

    @Test
    void testSaveStationManufacturer() {
        StationManufacturer stationManufacturer = jdbcStationManufacturerRepository.getById(UUID.fromString("42685117-0870-41f0-97d3-a58840a05bf9"))
                .withId(UUID.fromString("d25f211a-3cd6-448c-8351-d1ccf57cb66c"));

        jdbcStationManufacturerRepository.saveStationManufacturer(stationManufacturer);
        assertEquals(2, jpaStationManufacturerRepository.findAll().size());

    }

    @Test
    void testGetById() {
        StationManufacturer stationManufacturer = jdbcStationManufacturerRepository.getById(UUID.fromString("42685117-0870-41f0-97d3-a58840a05bf9"));
        assertNotNull(stationManufacturer);
    }

    @Test
    void testDeleteStationManufacturer() {
        StationManufacturer stationManufacturer = jdbcStationManufacturerRepository.getById(UUID.fromString("42685117-0870-41f0-97d3-a58840a05bf9"));
        jdbcStationManufacturerRepository.deleteStationManufacturer(stationManufacturer.getId());

        assertNull(jdbcStationManufacturerRepository.getById(UUID.fromString("42685117-0870-41f0-97d3-a58840a05bf9")));
    }

    @Test
    void testUpdate() {
        StationManufacturer stationManufacturer = jdbcStationManufacturerRepository.getById(UUID.fromString("42685117-0870-41f0-97d3-a58840a05bf9"));
        stationManufacturer.setStationRemark("Updated remark");
        jdbcStationManufacturerRepository.update(stationManufacturer);

        StationManufacturer updatedStationManufacturer = jdbcStationManufacturerRepository.getById(UUID.fromString("42685117-0870-41f0-97d3-a58840a05bf9"));

        assertNotNull(updatedStationManufacturer);
        assertEquals(updatedStationManufacturer.getStationRemark(), "Updated remark");
    }

    @Test
    void testGetAllStationManufacturers() {
        assertEquals(1, jdbcStationManufacturerRepository.getAllStationManufacturers().size());
    }
}
