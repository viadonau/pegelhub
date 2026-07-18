package at.pegelhub.connector.application;

import at.pegelhub.connector.persistence.ConnectorRepository;
import at.pegelhub.contact.application.ContactDeletionGuard;
import at.pegelhub.shared.error.ConflictException;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

@Component
class ConnectorContactDeletionGuard implements ContactDeletionGuard {

    private final ConnectorRepository connectors;

    ConnectorContactDeletionGuard(ConnectorRepository connectors) {
        this.connectors = requireNonNull(connectors);
    }

    @Override
    public void assertCanDelete(UUID contactId) {
        requireNonNull(contactId);
        if (connectors.existsReferencingContact(contactId)) {
            throw new ConflictException("Contact is still referenced by a connector.");
        }
    }
}
