package at.pegelhub.contact.application;

import at.pegelhub.contact.domain.Contact;
import at.pegelhub.contact.persistence.ContactRepository;
import at.pegelhub.shared.error.ConflictException;
import at.pegelhub.shared.error.NotFoundException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static at.pegelhub.testsupport.ExampleData.CONTACT;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

final class ContactServiceImplTest {

    private ContactServiceImpl contactService;
    private static final ContactRepository REPOSITORY = mock(ContactRepository.class);
    private static final ContactDeletionGuard DELETION_GUARD = mock(ContactDeletionGuard.class);

    @BeforeEach
    public void prepare() {
        contactService = new ContactServiceImpl(REPOSITORY, List.of(DELETION_GUARD));
        reset(REPOSITORY);
        reset(DELETION_GUARD);
    }

    @Test
    public void constructorWithNullArgsThrowsNPE() {
        assertThrows(NullPointerException.class, () -> new ContactServiceImpl(null, List.of()));
        assertThrows(NullPointerException.class, () -> new ContactServiceImpl(REPOSITORY, null));
    }

    @Test
    public void createContact() {
        when(REPOSITORY.saveContact(any())).thenReturn(CONTACT);

        Contact result = contactService.createContact(CONTACT);
        assertEquals(CONTACT, result);
        verify(REPOSITORY, times(1)).saveContact(any());
    }

    @Test
    public void getById() {
        when(REPOSITORY.getById(any())).thenReturn(CONTACT);

        Contact result = contactService.getContactById(UUID.randomUUID());
        assertEquals(CONTACT, result);
        verify(REPOSITORY, times(1)).getById(any());
    }

    @Test
    void getByIdThrowsNotFoundWhenContactIsMissing() {
        UUID id = UUID.randomUUID();
        when(REPOSITORY.getById(id)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> contactService.getContactById(id));
    }

    @Test
    void deleteThrowsNotFoundWithoutDeletingWhenContactIsMissing() {
        UUID id = UUID.randomUUID();
        when(REPOSITORY.getById(id)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> contactService.deleteContact(id));

        verifyNoInteractions(DELETION_GUARD);
        verify(REPOSITORY, never()).deleteContact(id);
    }

    @Test
    void deleteExistingContact() {
        UUID id = requireNonNull(CONTACT.getId());
        when(REPOSITORY.getById(id)).thenReturn(CONTACT);

        contactService.deleteContact(id);

        verify(DELETION_GUARD).assertCanDelete(id);
        verify(REPOSITORY).deleteContact(id);
    }


    @Test
    public void getAll() {
        when(REPOSITORY.getAllContacts()).thenReturn(List.of(CONTACT));

        List<Contact> result = contactService.getAllContacts();
        assertEquals(1, result.size());
        Assertions.assertThat(result).containsOnly(CONTACT);
        verify(REPOSITORY, times(1)).getAllContacts();
    }
    @Test
    public void deleteContactStopsWhenGuardRejectsDeletion() {
        UUID contactId = UUID.randomUUID();
        when(REPOSITORY.getById(contactId)).thenReturn(CONTACT);
        doThrow(new ConflictException("Contact is still referenced by a connector."))
                .when(DELETION_GUARD).assertCanDelete(contactId);

        assertThrows(ConflictException.class, () -> contactService.deleteContact(contactId));

        verify(DELETION_GUARD).assertCanDelete(contactId);
        verify(REPOSITORY, never()).deleteContact(any());
    }
}
