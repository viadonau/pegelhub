package at.pegelhub.stationowner.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class StationOwnerTest {

    private static final StationOwnerId ID = new StationOwnerId(UUID.fromString("ce86da06-e029-46b8-a44b-0dcd3b9d0acb"));

    @Test
    void rejectsMissingRequiredValues() {
        assertThrows(NullPointerException.class, () -> new StationOwner(null, "Hydro Org", null, null));
        assertThrows(NullPointerException.class, () -> new StationOwner(ID, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new StationOwner(ID, " ", null, null));
    }

    @Test
    void normalizesTextValues() {
        var owner = new StationOwner(ID, " Hydro Org ", " HO ", " notes ");

        assertThat(owner.name()).isEqualTo("Hydro Org");
        assertThat(owner.shortName()).isEqualTo("HO");
        assertThat(owner.notes()).isEqualTo("notes");
    }

    @Test
    void blankOptionalValuesBecomeAbsent() {
        var owner = new StationOwner(ID, "Hydro Org", " ", " ");

        assertThat(owner.shortName()).isNull();
        assertThat(owner.notes()).isNull();
    }

    @Test
    void createAssignsIdentity() {
        var owner = StationOwner.create("Hydro Org", null, null);

        assertThat(owner.id()).isNotNull();
        assertThat(owner.id().value()).isNotNull();
    }
}
