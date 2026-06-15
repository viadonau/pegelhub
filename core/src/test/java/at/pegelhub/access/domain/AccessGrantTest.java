package at.pegelhub.access.domain;

import at.pegelhub.connector.domain.ConnectorId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class AccessGrantTest {

    private static final AccessGrantId ID = new AccessGrantId(UUID.fromString("d688b999-e199-4f16-a33c-eaa711e6df5e"));
    private static final ConnectorId CONNECTOR_ID = new ConnectorId(UUID.fromString("40ba0479-b2f6-4d3c-b7b3-f125570dd57b"));
    private static final AccessResourceRef STATION_RESOURCE = new AccessResourceRef(
            AccessResourceType.STATION,
            UUID.fromString("63520e38-ecc6-4d63-94b5-005ec230f08a"));
    @Test
    void rejectsMissingRequiredValues() {
        assertThrows(NullPointerException.class,
                () -> new AccessGrant(null, CONNECTOR_ID, STATION_RESOURCE, AccessPermission.READ));
        assertThrows(NullPointerException.class,
                () -> new AccessGrant(ID, null, STATION_RESOURCE, AccessPermission.READ));
        assertThrows(NullPointerException.class,
                () -> new AccessGrant(ID, CONNECTOR_ID, null, AccessPermission.READ));
        assertThrows(NullPointerException.class,
                () -> new AccessGrant(ID, CONNECTOR_ID, STATION_RESOURCE, null));
    }

    @Test
    void createAssignsIdentity() {
        var grant = AccessGrant.create(CONNECTOR_ID, STATION_RESOURCE, AccessPermission.READ);

        assertThat(grant.id()).isNotNull();
        assertThat(grant.id().value()).isNotNull();
        assertThat(grant.connectorId()).isEqualTo(CONNECTOR_ID);
        assertThat(grant.resource()).isEqualTo(STATION_RESOURCE);
    }
}
