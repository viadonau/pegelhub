package com.stm.pegelhub.taker.persistence;

import com.stm.pegelhub.testsupport.JpaIntegrationTestBase;
import com.stm.pegelhub.taker.domain.Taker;
import com.stm.pegelhub.taker.persistence.JpaTakerRepository;
import com.stm.pegelhub.taker.persistence.JdbcTakerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final public class TakerPostgresIntegrationTest extends JpaIntegrationTestBase {

    private JdbcTakerRepository jdbcTakerRepository;

    @Autowired
    private JpaTakerRepository jpaTakerRepository;

    @BeforeEach
    void prepare() {
        jdbcTakerRepository = new JdbcTakerRepository(jpaTakerRepository);
    }

    @Test
    void testSaveTaker() {
        Taker taker = jdbcTakerRepository.getById(UUID.fromString("1c0fd27c-5e46-438c-92c1-cdb1ff6ccb50"))
                .withId(UUID.fromString("47ad9818-9c46-40b7-9e8b-82c13aef040b"));

        jdbcTakerRepository.saveTaker(taker);
        assertEquals(2, jpaTakerRepository.findAll().size());

    }

    @Test
    void testGetById() {
        Taker taker = jdbcTakerRepository.getById(UUID.fromString("1c0fd27c-5e46-438c-92c1-cdb1ff6ccb50"));
        assertNotNull(taker);
    }

    @Test
    void testDeleteTaker() {
        Taker taker = jdbcTakerRepository.getById(UUID.fromString("1c0fd27c-5e46-438c-92c1-cdb1ff6ccb50"));
        jdbcTakerRepository.deleteTaker(taker.getId());

        assertNull(jdbcTakerRepository.getById(UUID.fromString("1c0fd27c-5e46-438c-92c1-cdb1ff6ccb50")));
    }

    @Test
    void testUpdate() {
        Duration weekly = Duration.of(7, ChronoUnit.DAYS);
        Taker taker = jdbcTakerRepository.getById(UUID.fromString("1c0fd27c-5e46-438c-92c1-cdb1ff6ccb50"));
        taker.setRefreshRate(weekly);
        jdbcTakerRepository.update(taker);

        Taker updatedTaker = jdbcTakerRepository.getById(UUID.fromString("1c0fd27c-5e46-438c-92c1-cdb1ff6ccb50"));

        assertNotNull(updatedTaker);
        assertEquals(updatedTaker.getRefreshRate(), weekly);
    }

    @Test
    void testGetAllTakers() {
        assertEquals(1, jdbcTakerRepository.getAllTakers().size());
    }
}
