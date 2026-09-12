package at.pegelhub.connector.iec.datapoints;

import at.pegelhub.lib.config.MappingDirection;
import at.pegelhub.lib.model.MeasurementRepresentation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IecMappingIndexTest {

    @Test
    void shouldLoadProtocolToCoreAndCoreToProtocolMappings() {
        var inbound = mapping(1001, MappingDirection.EXTERNAL_TO_CORE);
        var outbound = new DataPointMapping(
                2002, UUID.randomUUID(), MappingDirection.CORE_TO_EXTERNAL,
                MeasurementRepresentation.METRES_ABOVE_ADRIA);
        var index = new IecMappingIndex(List.of(inbound, outbound));

        assertThat(index.protocolToCoreIoas()).containsExactly(1001);
        assertThat(index.coreToProtocolMappings()).containsExactly(outbound);
        assertThat(index.getTimeSeriesId(1001)).contains(inbound.timeSeriesId());
        assertThat(index.getTimeSeriesId(2002)).contains(outbound.timeSeriesId());
        assertThat(index.getTimeSeriesId(9999)).isEmpty();
    }

    @Test
    void keepsAnImmutableSnapshotInConfiguredOutputOrder() {
        var first = mapping(2002, MappingDirection.CORE_TO_EXTERNAL);
        var second = mapping(1001, MappingDirection.CORE_TO_EXTERNAL);
        var configuration = new ArrayList<>(List.of(first, second));
        var index = new IecMappingIndex(configuration);
        configuration.clear();

        assertThat(index.coreToProtocolMappings()).containsExactly(first, second);
        assertThatThrownBy(() -> index.coreToProtocolMappings().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> index.protocolToCoreIoas().add(9999))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldFailOnDuplicateIoas() throws Exception {
        assertThatThrownBy(() -> new IecMappingIndex(List.of(
                mapping(1234, MappingDirection.EXTERNAL_TO_CORE),
                mapping(1234, MappingDirection.CORE_TO_EXTERNAL))))
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
