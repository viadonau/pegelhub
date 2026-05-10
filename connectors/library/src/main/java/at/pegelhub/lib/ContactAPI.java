package at.pegelhub.lib;

import at.pegelhub.lib.model.Contact;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * API spec for contact communication.
 */
public interface ContactAPI {
    /**
     * @return a {@code Collection} of all known contacts
     */
    Collection<Contact> getContacts();

    /**
     * @param uuid the identifier of a {@code Contact} entry
     * @return {@code Optional} with {@code Contact} or empty {@code Optional} if none match the {@param uuid}.
     */
    Optional<Contact> getContactByUUID(UUID uuid);

    /**
     * Sends a {@code Contact} to the core instance.
     * Throws {@code RuntimeException} if any errors occur.
     * @param contact the {@code Contact} to send
     */
    void sendContact(Contact contact);
}
