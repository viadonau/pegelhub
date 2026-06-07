package at.pegelhub.access.persistence;

import at.pegelhub.access.domain.AccessGrant;
import at.pegelhub.access.domain.AccessGrantId;
import at.pegelhub.access.domain.AccessPermission;
import at.pegelhub.access.domain.AccessResourceRef;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.testsupport.JpaIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(JpaAccessGrantRepositoryAdapter.class)
final class AccessGrantRepositoryIntegrationTest extends JpaIntegrationTestBase {

    private static final ConnectorId CONNECTOR_ID = new ConnectorId(UUID.fromString("cb52832c-ad7a-45f8-bd75-26df8715523b"));
    private static final ConnectorId OTHER_CONNECTOR_ID = new ConnectorId(UUID.fromString("f2f9f89b-7793-4b49-bef6-34ca3c7d3e33"));
    private static final StationId STATION_ID = new StationId(UUID.fromString("64d6f5cd-7af4-4d76-88e8-e01a2c7d8463"));
    private static final AccessGrantId GRANT_ID = new AccessGrantId(UUID.fromString("9de35379-193e-4419-8cf9-34893fa387d0"));

    @Autowired
    private AccessGrantRepository accessGrants;

    @Test
    void savesLoadsAndFiltersAccessGrant() {
        var matching = grant(GRANT_ID, CONNECTOR_ID);
        var other = grant(new AccessGrantId(UUID.fromString("3d7ffcb2-6468-455a-82e8-ec29ecf829af")), OTHER_CONNECTOR_ID);

        accessGrants.save(matching);
        accessGrants.save(other);

        assertThat(accessGrants.findById(GRANT_ID)).contains(matching);
        assertThat(accessGrants.findAll()).contains(matching, other);
        assertThat(accessGrants.findByConnectorId(CONNECTOR_ID)).containsExactly(matching);
    }

    private static AccessGrant grant(AccessGrantId id, ConnectorId connectorId) {
        return new AccessGrant(
                id,
                connectorId,
                AccessResourceRef.station(STATION_ID),
                AccessPermission.MANAGE,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-12-31T00:00:00Z"),
                true);
    }
}
