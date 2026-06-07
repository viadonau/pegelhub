package at.pegelhub.stationowner.persistence;

import at.pegelhub.stationowner.domain.StationOwner;
import at.pegelhub.stationowner.domain.StationOwnerId;
import at.pegelhub.testsupport.JpaIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(JpaStationOwnerRepositoryAdapter.class)
final class StationOwnerRepositoryIntegrationTest extends JpaIntegrationTestBase {

    private static final StationOwnerId OWNER_ID = new StationOwnerId(
            UUID.fromString("e0d9cd73-84c5-4f43-a103-99023ccab78c"));

    @Autowired
    private StationOwnerRepository stationOwners;

    @Test
    void savesAndLoadsStationOwner() {
        var stationOwner = new StationOwner(OWNER_ID, "Hydro Org", "HO", "notes");

        stationOwners.save(stationOwner);

        assertThat(stationOwners.findById(OWNER_ID)).contains(stationOwner);
        assertThat(stationOwners.findAll()).contains(stationOwner);
    }
}
