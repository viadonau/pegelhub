package at.pegelhub.connector.application;

import at.pegelhub.connector.persistence.ConnectorRepository;
import at.pegelhub.shared.error.ConflictException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConnectorContactDeletionGuardTest {

    private final ConnectorRepository connectors = mock(ConnectorRepository.class);
    private final ConnectorContactDeletionGuard guard = new ConnectorContactDeletionGuard(connectors);

    @Test
    void allowsContactWhenNoConnectorReferencesIt() {
        UUID contactId = UUID.randomUUID();

        assertDoesNotThrow(() -> guard.assertCanDelete(contactId));
    }

    @Test
    void rejectsContactWhenConnectorReferencesIt() {
        UUID contactId = UUID.randomUUID();
        when(connectors.existsReferencingContact(contactId)).thenReturn(true);

        assertThrows(ConflictException.class, () -> guard.assertCanDelete(contactId));
    }
}
