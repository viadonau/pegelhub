package at.pegelhub.access.persistence;

import at.pegelhub.access.domain.AccessGrant;
import at.pegelhub.access.domain.AccessGrantId;
import at.pegelhub.access.domain.AccessPermission;
import at.pegelhub.access.domain.AccessResourceRef;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.testsupport.JpaIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(AccessGrantRepositoryAdapter.class)
final class AccessGrantRepositoryIntegrationTest extends JpaIntegrationTestBase {

    private static final UUID CONTACT_ID = UUID.fromString("7cb26fac-dce2-4239-b151-1a3aa4111947");
    private static final ConnectorId CONNECTOR_ID = new ConnectorId(UUID.fromString("cb52832c-ad7a-45f8-bd75-26df8715523b"));
    private static final ConnectorId OTHER_CONNECTOR_ID = new ConnectorId(UUID.fromString("f2f9f89b-7793-4b49-bef6-34ca3c7d3e33"));
    private static final StationId STATION_ID = new StationId(UUID.fromString("64d6f5cd-7af4-4d76-88e8-e01a2c7d8463"));
    private static final AccessGrantId GRANT_ID = new AccessGrantId(UUID.fromString("9de35379-193e-4419-8cf9-34893fa387d0"));

    @Autowired
    private AccessGrantRepository accessGrants;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void insertConnectors() {
        jdbc.update("insert into contact (id) values (?)", CONTACT_ID);
        insertConnector(CONNECTOR_ID, "connector-1");
        insertConnector(OTHER_CONNECTOR_ID, "connector-2");
    }

    @Test
    void savesLoadsAndFiltersAccessGrant() {
        var matching = grant(GRANT_ID, CONNECTOR_ID);
        var other = grant(new AccessGrantId(UUID.fromString("3d7ffcb2-6468-455a-82e8-ec29ecf829af")), OTHER_CONNECTOR_ID);

        accessGrants.save(matching);
        accessGrants.save(other);

        assertThat(accessGrants.findById(GRANT_ID)).contains(matching);
        assertThat(accessGrants.findByAssignment(
                CONNECTOR_ID,
                AccessResourceRef.station(STATION_ID),
                AccessPermission.READ)).contains(matching);
        assertThat(accessGrants.findAll()).contains(matching, other);
        assertThat(accessGrants.findByConnectorId(CONNECTOR_ID)).containsExactly(matching);
    }

    @Test
    void saveOrFindByAssignmentReturnsExistingGrantForDuplicateAssignment() {
        var existing = grant(GRANT_ID, CONNECTOR_ID);
        var duplicate = grant(
                new AccessGrantId(UUID.fromString("d8440189-090b-4db0-ad93-8cb6d140a67a")),
                CONNECTOR_ID);

        accessGrants.save(existing);

        assertThat(accessGrants.saveOrFindByAssignment(duplicate)).isEqualTo(existing);
        assertThat(accessGrants.findAll()).containsExactly(existing);
    }

    private static AccessGrant grant(AccessGrantId id, ConnectorId connectorId) {
        return new AccessGrant(
                id,
                connectorId,
                AccessResourceRef.station(STATION_ID),
                AccessPermission.READ);
    }

    private void insertConnector(ConnectorId id, String connectorNumber) {
        jdbc.update("""
                        insert into connector (
                            id,
                            manufacturer_id,
                            connector_number,
                            type_description,
                            software_version,
                            works_from_data_version,
                            data_definition,
                            software_manufacturer_id,
                            technically_responsible_id,
                            operating_company_id)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id.value(),
                CONTACT_ID,
                connectorNumber,
                "type",
                "1.0.0",
                "1.0.0",
                "definition",
                CONTACT_ID,
                CONTACT_ID,
                CONTACT_ID);
    }
}
