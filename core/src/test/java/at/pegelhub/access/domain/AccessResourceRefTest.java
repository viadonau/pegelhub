package at.pegelhub.access.domain;

import at.pegelhub.station.domain.StationId;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class AccessResourceRefTest {

    private static final UUID RESOURCE_ID = UUID.fromString("1375639c-8b09-4ff8-92d9-fe8cb06d8e81");

    @Test
    void rejectsMissingTypeOrId() {
        assertThrows(NullPointerException.class, () -> new AccessResourceRef(null, RESOURCE_ID));
        assertThrows(NullPointerException.class, () -> new AccessResourceRef(AccessResourceType.STATION, null));
    }

    @Test
    void createsStationResourceReference() {
        var ref = AccessResourceRef.station(new StationId(RESOURCE_ID));

        assertThat(ref.type()).isEqualTo(AccessResourceType.STATION);
        assertThat(ref.id()).isEqualTo(RESOURCE_ID);
    }

    @Test
    void createsTimeSeriesResourceReference() {
        var ref = AccessResourceRef.timeSeries(new TimeSeriesId(RESOURCE_ID));

        assertThat(ref.type()).isEqualTo(AccessResourceType.TIME_SERIES);
        assertThat(ref.id()).isEqualTo(RESOURCE_ID);
    }
}
