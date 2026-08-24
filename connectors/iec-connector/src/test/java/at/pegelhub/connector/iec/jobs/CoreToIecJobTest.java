package at.pegelhub.connector.iec.jobs;

import at.pegelhub.connector.iec.datapoints.DataPointMapping;
import at.pegelhub.connector.iec.datapoints.IecMappingIndex;
import at.pegelhub.connector.iec.iec.IecClient;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.model.Measurement;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static at.pegelhub.lib.config.MappingDirection.CORE_TO_EXTERNAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CoreToIecJobTest {

    private static Measurement m(double v) {
        return new Measurement(UUID.fromString("395c0232-d110-40fd-bd7f-2bb4a0f2009d"), Instant.now(), v);
    }

    @Test
    void shouldSendOnlyWhenLatestMeasurementIsPresent() {
        // Given
        IecClient iec = mock(IecClient.class);
        IecMappingIndex reg = mock(IecMappingIndex.class);

        PegelHubClient core = mock(PegelHubClient.class);

        when(reg.coreToProtocolIoas()).thenReturn(Set.of(11, 22, 33));

        when(reg.getTimeSeriesId(11)).thenReturn(Optional.of(UUID.fromString("395c0232-d110-40fd-bd7f-2bb4a0f2009d")));
        when(core.getLatestMeasurementOfTimeSeries(UUID.fromString("395c0232-d110-40fd-bd7f-2bb4a0f2009d"))).thenReturn(Optional.of(m(1.1)));

        when(reg.getTimeSeriesId(22)).thenReturn(Optional.of(UUID.fromString("abdc0232-d110-40fd-bd7f-2bb4a0f2009d")));
        when(core.getLatestMeasurementOfTimeSeries(UUID.fromString("abdc0232-d110-40fd-bd7f-2bb4a0f2009d"))).thenReturn(Optional.empty());

        when(reg.getTimeSeriesId(33)).thenReturn(Optional.empty());

        CoreToIecJob job = new CoreToIecJob(iec, reg, core);

        // When
        job.run();

        // Then
        verify(iec, times(1)).sendMeasurement(eq(11), any(Measurement.class));
        verify(iec, never()).sendMeasurement(eq(22), any());
        verify(iec, never()).sendMeasurement(eq(33), any());
    }

    @Test
    void shouldConvertCanonicalCentimetresToMetresAboveAdria() {
        UUID timeSeriesId = UUID.fromString("395c0232-d110-40fd-bd7f-2bb4a0f2009d");
        IecClient iec = mock(IecClient.class);
        IecMappingIndex mappings = new IecMappingIndex(List.of(new DataPointMapping(
                66059, timeSeriesId, CORE_TO_EXTERNAL, new BigDecimal("152.68"))));
        PegelHubClient core = mock(PegelHubClient.class);
        when(core.getLatestMeasurementOfTimeSeries(timeSeriesId)).thenReturn(Optional.of(m(288)));

        new CoreToIecJob(iec, mappings, core).run();

        ArgumentCaptor<Measurement> measurement = ArgumentCaptor.forClass(Measurement.class);
        verify(iec).sendMeasurement(eq(66059), measurement.capture());
        assertThat(measurement.getValue().getValue()).isEqualTo(155.56);
    }
}
