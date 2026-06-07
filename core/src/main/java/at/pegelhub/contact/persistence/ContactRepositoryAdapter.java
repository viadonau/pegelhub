package at.pegelhub.contact.persistence;

import at.pegelhub.shared.persistence.DomainToEntityConverter;
import at.pegelhub.shared.persistence.EntityToDomainConverter;
import at.pegelhub.shared.persistence.*;

import at.pegelhub.contact.domain.Contact;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * JDBC Implementation of the Interface {@code ContactRepository}.
 */
@Repository
public class ContactRepositoryAdapter implements ContactRepository {
    private final SpringDataContactRepository jpaContactRepository;

    public ContactRepositoryAdapter(SpringDataContactRepository jpaContactRepository) {
        this.jpaContactRepository = jpaContactRepository;
    }

    /**
     *
     * @param contact {@link Contact} to save.
     * @return the saved {@link Contact}
     */
    @Override
    public Contact saveContact(Contact contact) {
        if (contact.getId() == null) {
            contact = contact.withId(UUID.randomUUID());
        }
        return EntityToDomainConverter.convert(jpaContactRepository.save(DomainToEntityConverter.convert(contact)));
    }

    /**
     * @param uuid {@link UUID} of the {@link Contact}.
     * @return the corresponding {@link Contact} to the given {@link UUID}
     */
    @Override
    public Contact getById(UUID uuid) {
        return jpaContactRepository.findById(uuid).map(EntityToDomainConverter::convert).orElse(null);
    }

    /**
     * @return all saved {@link Contact}s
     */
    @Override
    public List<Contact> getAllContacts() {
        return EntityToDomainConverter.convert(jpaContactRepository.findAll());
    }

    /**
     * @param contact {@link Contact} to update.
     * @return the updated {@link Contact}
     */
    @Override
    public Contact update(Contact contact) {
        return EntityToDomainConverter.convert(jpaContactRepository.save(DomainToEntityConverter.convert(contact)));
    }

    /**
     * @param uuid {@link UUID} of the {@link Contact} to delete.
     */
    @Override
    public void deleteContact(UUID uuid) {
        jpaContactRepository.delete(jpaContactRepository.findById(uuid).orElseThrow());
    }
}
