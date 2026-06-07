package at.pegelhub.connector.iec.jobs;

import at.pegelhub.connector.iec.datapoints.DataPointRegistry;
import at.pegelhub.connector.iec.iec.IecClient;
import at.pegelhub.lib.PegelHubCommunicator;
import at.pegelhub.lib.model.Measurement;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.mockito.Mockito.*;

class IecWriteJobTest {

    private static Measurement m(double v) {
        return new Measurement(UUID.fromString("395c0232-d110-40fd-bd7f-2bb4a0f2009d"), Instant.now(), v);
    }

    @Test
    void shouldSendOnlyWhenLatestMeasurementIsPresent() {
        // Given
        IecClient iec = mock(IecClient.class);
        DataPointRegistry reg = mock(DataPointRegistry.class);

        PegelHubCommunicator c1 = mock(PegelHubCommunicator.class);
        PegelHubCommunicator c2 = mock(PegelHubCommunicator.class);

        when(reg.takerIoas()).thenReturn(Set.of(11, 22, 33));

        when(reg.getTaker(11)).thenReturn(Optional.of(c1));
        when(reg.getTimeSeriesId(11)).thenReturn(Optional.of(UUID.fromString("395c0232-d110-40fd-bd7f-2bb4a0f2009d")));
        when(c1.getLatestMeasurementOfTimeSeries(UUID.fromString("395c0232-d110-40fd-bd7f-2bb4a0f2009d"))).thenReturn(Optional.of(m(1.1)));

        when(reg.getTaker(22)).thenReturn(Optional.of(c2));
        when(reg.getTimeSeriesId(22)).thenReturn(Optional.of(UUID.fromString("abdc0232-d110-40fd-bd7f-2bb4a0f2009d")));
        when(c2.getLatestMeasurementOfTimeSeries(UUID.fromString("abdc0232-d110-40fd-bd7f-2bb4a0f2009d"))).thenReturn(Optional.empty());

        when(reg.getTaker(33)).thenReturn(Optional.empty());

        IecWriteJob job = new IecWriteJob(iec, reg);

        // When
        job.run();

        // Then
        verify(iec, times(1)).sendMeasurement(eq(11), any(Measurement.class));
        verify(iec, never()).sendMeasurement(eq(22), any());
        verify(iec, never()).sendMeasurement(eq(33), any());
    }
}
