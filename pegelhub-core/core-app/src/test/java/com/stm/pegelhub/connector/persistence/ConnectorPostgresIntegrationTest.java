package com.stm.pegelhub.connector.persistence;

import com.stm.pegelhub.testsupport.JpaIntegrationTestBase;
import com.stm.pegelhub.connector.domain.Connector;
import com.stm.pegelhub.auth.application.AuthTokenIdHolder;
import com.stm.pegelhub.connector.persistence.JpaConnectorRepository;
import com.stm.pegelhub.connector.persistence.JdbcConnectorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final public class ConnectorPostgresIntegrationTest extends JpaIntegrationTestBase {

    private JdbcConnectorRepository jdbcConnectorRepository;
    @Autowired
    private JpaConnectorRepository jpaConnectorRepository;

    @BeforeEach
    void prepare() {
        jdbcConnectorRepository = new JdbcConnectorRepository(jpaConnectorRepository);
    }

    @Test
    void testSaveConnector() {
        Connector connector = jdbcConnectorRepository.getById(UUID.fromString("74a48b34-8a3e-4f5b-aeca-ba006a0e6692"))
                .withId(UUID.fromString("3ec98785-d968-4307-bc95-ed2d065f63b9"));

        AuthTokenIdHolder.set(UUID.fromString("3c727bd5-9822-482e-996b-b8ead0ce687d"));
        jdbcConnectorRepository.saveConnector(connector);
        assertEquals(3, jpaConnectorRepository.findAll().size());
    }

    @Test
    void testGetById() {
        Connector connector = jdbcConnectorRepository.getById(UUID.fromString("74a48b34-8a3e-4f5b-aeca-ba006a0e6692"));
        assertNotNull(connector);
    }


    @Test
    void testDeleteConnector() {
        Connector connector = jdbcConnectorRepository.getById(UUID.fromString("74a48b34-8a3e-4f5b-aeca-ba006a0e6692"));
        jdbcConnectorRepository.deleteConnector(connector.getId());

        assertNull(jdbcConnectorRepository.getById(UUID.fromString("74a48b34-8a3e-4f5b-aeca-ba006a0e6692")));
    }


    @Test
    void testUpdate() {
        Connector connector = jdbcConnectorRepository.getById(UUID.fromString("74a48b34-8a3e-4f5b-aeca-ba006a0e6692"));
        connector.setSoftwareVersion("3.5.2");
        jdbcConnectorRepository.update(connector);

        Connector updatedConnector = jdbcConnectorRepository.getById(UUID.fromString("74a48b34-8a3e-4f5b-aeca-ba006a0e6692"));

        assertNotNull(updatedConnector);
        assertEquals(updatedConnector.getSoftwareVersion(), "3.5.2");
    }

    @Test
    void testGetAllConnectors() {
        assertEquals(2, jdbcConnectorRepository.getAllConnectors().size());
    }
}
