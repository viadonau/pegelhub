package at.pegelhub.contact.application;

import java.util.UUID;

public interface ContactDeletionGuard {

    void assertCanDelete(UUID contactId);
}
