package at.pegelhub.connector.iec.jobs;

import at.pegelhub.connector.iec.datapoints.DataPointMapping;
import at.pegelhub.connector.iec.datapoints.IecMappingIndex;
import at.pegelhub.connector.iec.iec.IecClient;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.model.Measurement;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static at.pegelhub.lib.config.MappingDirection.CORE_TO_EXTERNAL;
import static at.pegelhub.lib.model.MeasurementRepresentation.*;
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
        when(reg.getOutputRepresentation(anyInt())).thenReturn(CANONICAL);

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
    void continuesOtherIoasAfterReadOrSendFailure() {
        var iec = mock(IecClient.class);
        var mappings = mock(IecMappingIndex.class);
        when(mappings.getOutputRepresentation(anyInt())).thenReturn(CANONICAL);
        var core = mock(PegelHubClient.class);
        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        var third = UUID.randomUUID();
        when(mappings.coreToProtocolIoas()).thenReturn(new LinkedHashSet<>(List.of(11, 22, 33)));
        when(mappings.getTimeSeriesId(11)).thenReturn(Optional.of(first));
        when(mappings.getTimeSeriesId(22)).thenReturn(Optional.of(second));
        when(mappings.getTimeSeriesId(33)).thenReturn(Optional.of(third));
        when(core.getLatestMeasurementOfTimeSeries(first)).thenThrow(new RuntimeException("Core unavailable"));
        when(core.getLatestMeasurementOfTimeSeries(second)).thenReturn(Optional.of(m(2)));
        when(core.getLatestMeasurementOfTimeSeries(third)).thenReturn(Optional.of(m(3)));
        doThrow(new IllegalStateException("IEC unavailable")).when(iec).sendMeasurement(eq(22), any());

        new CoreToIecJob(iec, mappings, core).run();

        verify(iec).sendMeasurement(eq(33), any());
    }

    @Test
    void requestsAbsoluteWaterLevelFromCoreWithoutLocalConversion() {
        UUID timeSeriesId = UUID.fromString("395c0232-d110-40fd-bd7f-2bb4a0f2009d");
        IecClient iec = mock(IecClient.class);
        IecMappingIndex mappings = new IecMappingIndex(List.of(new DataPointMapping(
                66059, timeSeriesId, CORE_TO_EXTERNAL, METRES_ABOVE_ADRIA)));
        PegelHubClient core = mock(PegelHubClient.class);
        var converted = m(155.56);
        when(core.getLatestMeasurementOfTimeSeries(timeSeriesId, METRES_ABOVE_ADRIA)).thenReturn(Optional.of(converted));

        new CoreToIecJob(iec, mappings, core).run();

        verify(iec).sendMeasurement(66059, converted);
        verify(core, never()).getLatestMeasurementOfTimeSeries(timeSeriesId);
    }

    @Test
    void representationFailureNeverFallsBackToSendingCanonicalValues() {
        UUID id = UUID.randomUUID();
        var iec = mock(IecClient.class);
        var core = mock(PegelHubClient.class);
        var mappings = new IecMappingIndex(List.of(new DataPointMapping(123, id, CORE_TO_EXTERNAL, METRES_ABOVE_ADRIA)));
        when(core.getLatestMeasurementOfTimeSeries(id, METRES_ABOVE_ADRIA))
                .thenThrow(new IllegalStateException("Core did not confirm representation"));
        new CoreToIecJob(iec, mappings, core).run();
        verifyNoInteractions(iec);
        verify(core, never()).getLatestMeasurementOfTimeSeries(id);
    }
}
