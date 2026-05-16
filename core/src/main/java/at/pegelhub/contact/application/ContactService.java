package at.pegelhub.contact.application;

import at.pegelhub.contact.domain.Contact;

import java.util.List;
import java.util.UUID;

/**
 * Service for all {@code Contact}s.
 */
public interface ContactService {

    /**
     * Creates a contact.
     *
     * @param contact to save.
     * @return the saved contact.
     */
    Contact createContact(Contact contact);

    /**
     * Get a contact by its id.
     *
     * @param uuid of the contact.
     * @return the found contact.
     */
    Contact getContactById(UUID uuid);

    /**
     * Get all contacts.
     *
     * @return the found contacts.
     */
    List<Contact> getAllContacts();

    /**
     * Deletes a contact by its id.
     *
     * @param uuid of the contact to delete.
     */
    void deleteContact(UUID uuid);
}
