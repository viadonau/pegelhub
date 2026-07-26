package at.pegelhub.contact.application;

import at.pegelhub.contact.domain.Contact;
import at.pegelhub.contact.persistence.ContactRepository;
import at.pegelhub.shared.error.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * Default implementation for {@code ContactService}.
 */
@Service
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;
    private final List<ContactDeletionGuard> deletionGuards;

    public ContactServiceImpl(ContactRepository contactRepository, List<ContactDeletionGuard> deletionGuards) {
        this.contactRepository = requireNonNull(contactRepository);
        this.deletionGuards = List.copyOf(requireNonNull(deletionGuards));
    }

    /**
     * @param contact to save.
     * @return the saved {@link Contact}
     */
    @Override
    public Contact createContact(Contact contact) {
        return contactRepository.saveContact(contact);
    }

    /**
     * @param uuid of the contact.
     * @return the corresponding {@link Contact} to the specified {@link UUID}
     */
    @Override
    public Contact getContactById(UUID uuid) {
        requireNonNull(uuid);
        Contact contact = contactRepository.getById(uuid);
        if (contact == null) {
            throw new NotFoundException("Contact not found: " + uuid);
        }
        return contact;
    }

    /**
     * @return all saved {@link Contact}s
     */
    @Override
    public List<Contact> getAllContacts() {
        return contactRepository.getAllContacts();
    }

    /**
     * @param uuid of the contact to delete.
     */
    @Override
    public void deleteContact(UUID uuid) {
        getContactById(uuid);
        deletionGuards.forEach(guard -> guard.assertCanDelete(uuid));
        contactRepository.deleteContact(uuid);
    }
}
