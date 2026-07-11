package at.pegelhub.connector.iec.datapoints;

import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.config.MappingDirection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DataPointRegistryTest {

    @Test
    void shouldLoadProtocolToCoreAndCoreToProtocolMappings() throws Exception {
        PegelHubClient client = mock(PegelHubClient.class);

        DataPointRegistry reg = new DataPointRegistry(List.of(
                mapping(1001, MappingDirection.EXTERNAL_TO_CORE),
                mapping(2002, MappingDirection.CORE_TO_EXTERNAL)), client);

        assertThat(reg.protocolToCoreIoas()).containsExactly(1001);
        assertThat(reg.coreToProtocolIoas()).containsExactly(2002);
        assertThat(reg.getProtocolToCoreClient(1001)).contains(client);
        assertThat(reg.getCoreToProtocolClient(2002)).contains(client);
        assertThat(reg.getTimeSeriesId(1001)).contains(UUID.fromString("395c0232-d110-40fd-bd7f-2bb4a0f2009d"));
        assertThat(reg.getProtocolToCoreClient(9999)).isEmpty();

        reg.close();
        verify(client, times(1)).close();
    }

    @Test
    void shouldFailOnDuplicateIoas() throws Exception {
        assertThatThrownBy(() -> new DataPointRegistry(List.of(
                mapping(1234, MappingDirection.EXTERNAL_TO_CORE),
                mapping(1234, MappingDirection.CORE_TO_EXTERNAL)), mock(PegelHubClient.class)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate IOA 1234");
    }

    @Test
    void shouldFailOnMissingRequiredFields() {
        assertThatThrownBy(() -> new DataPointMapping(null,
                UUID.fromString("395c0232-d110-40fd-bd7f-2bb4a0f2009d"),
                MappingDirection.EXTERNAL_TO_CORE))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("iecIoa");
    }

    private DataPointMapping mapping(int ioa, MappingDirection direction) {
        return new DataPointMapping(
                ioa,
                UUID.fromString("395c0232-d110-40fd-bd7f-2bb4a0f2009d"),
                direction);
    }
}
