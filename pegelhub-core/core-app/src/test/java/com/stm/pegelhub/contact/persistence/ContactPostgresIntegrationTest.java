package com.stm.pegelhub.contact.persistence;

import com.stm.pegelhub.testsupport.JpaIntegrationTestBase;
import com.stm.pegelhub.contact.domain.Contact;
import com.stm.pegelhub.contact.persistence.JpaContactRepository;
import com.stm.pegelhub.contact.persistence.JdbcContactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final public class ContactPostgresIntegrationTest extends JpaIntegrationTestBase {

    private JdbcContactRepository jdbcContactRepository;

    @Autowired
    private JpaContactRepository jpaContactRepository;

    @BeforeEach
    void prepare() {
        jdbcContactRepository = new JdbcContactRepository(jpaContactRepository);
    }

    @Test
    void testSaveContact() {
        Contact contact = jdbcContactRepository.getById(UUID.fromString("f68c2af4-86b1-46df-a4f0-b0450d4b6a8c"));
        contact = contact.withId(UUID.fromString("8e87c393-b5b5-4aa9-97b7-3026ebc68ce3"));

        jdbcContactRepository.saveContact(contact);
        assertEquals(2, jpaContactRepository.findAll().size());

    }

    @Test
    void testGetById() {
        Contact contact = jdbcContactRepository.getById(UUID.fromString("f68c2af4-86b1-46df-a4f0-b0450d4b6a8c"));
        assertNotNull(contact);
    }

    @Test
    void testDeleteContact() {
        Contact contact = jdbcContactRepository.getById(UUID.fromString("f68c2af4-86b1-46df-a4f0-b0450d4b6a8c"));
        jdbcContactRepository.deleteContact(contact.getId());

        assertNull(jdbcContactRepository.getById(UUID.fromString("f68c2af4-86b1-46df-a4f0-b0450d4b6a8c")));
    }

    @Test
    void testUpdate() {
        Contact contact = jdbcContactRepository.getById(UUID.fromString("f68c2af4-86b1-46df-a4f0-b0450d4b6a8c"));
        contact.setEmergencyNumber("144");
        jdbcContactRepository.update(contact);

        Contact updatedContact = jdbcContactRepository.getById(UUID.fromString("f68c2af4-86b1-46df-a4f0-b0450d4b6a8c"));

        assertNotNull(updatedContact);
        assertEquals(updatedContact.getEmergencyNumber(), "144");
    }

    @Test
    void testGetAllContacts() {
        assertEquals(1, jdbcContactRepository.getAllContacts().size());
    }

}
