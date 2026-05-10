package at.pegelhub.connector.iec.jobs;

import at.pegelhub.connector.iec.datapoints.DataPointRegistry;
import at.pegelhub.connector.iec.iec.IecClient;
import at.pegelhub.lib.PegelHubCommunicator;
import at.pegelhub.lib.model.Measurement;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.mockito.Mockito.*;

class IecWriteJobTest {

    private static Measurement m(double v) {
        Map<String, Double> fields = new HashMap<>();
        fields.put("value", v);
        return new Measurement(LocalDateTime.now(ZoneOffset.UTC), fields, new HashMap<>());
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
        when(c1.getLatestMeasurementOfStation()).thenReturn(Optional.of(m(1.1)));

        when(reg.getTaker(22)).thenReturn(Optional.of(c2));
        when(c2.getLatestMeasurementOfStation()).thenReturn(Optional.empty());

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
