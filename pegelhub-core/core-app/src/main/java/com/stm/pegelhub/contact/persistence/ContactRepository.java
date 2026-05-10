package com.stm.pegelhub.contact.persistence;

import com.stm.pegelhub.contact.domain.Contact;

import java.util.List;
import java.util.UUID;

/**
 * Repository for all {@code Contact}s.
 */
public interface ContactRepository {

    /**
     * Saves a contact to the repository.
     *
     * @param contact to save.
     * @return the saved contact.
     */
    Contact saveContact(Contact contact);

    /**
     * Get a contact from the repository by its id.
     *
     * @param uuid of the contact.
     * @return the found contact.
     */
    Contact getById(UUID uuid);

    /**
     * Get all contacts stored in the repository.
     *
     * @return the found contacts.
     */
    List<Contact> getAllContacts();

    /**
     * Updates a contact in the repository.
     *
     * @param contact to update.
     * @return the updated contact.
     */
    Contact update(Contact contact);

    /**
     * Deletes a contact by its id.
     *
     * @param uuid of the contact to delete.
     */
    void deleteContact(UUID uuid);
}

