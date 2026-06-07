package at.pegelhub.connector.iec.jobs;

import at.pegelhub.connector.iec.datapoints.DataPointRegistry;
import at.pegelhub.connector.iec.iec.IecClient;
import at.pegelhub.lib.PegelHubCommunicator;
import at.pegelhub.lib.model.Measurement;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThatCode;

class IecReadJobTest {

    private static Measurement m(double v) {
        return new Measurement(null, Instant.now(), v);
    }

    @Test
    void shouldSendEachGroupToSupplierAndIgnoreMissingOnes() throws Exception {
        // Given
        IecClient client = mock(IecClient.class);
        DataPointRegistry registry = mock(DataPointRegistry.class);
        PegelHubCommunicator comm = mock(PegelHubCommunicator.class);

        Map<Integer, List<Measurement>> grouped = Map.of(
                42, List.of(m(10), m(11)),
                314, List.of(m(3))
        );

        when(client.drainGroupedMeasurements()).thenReturn(grouped);
        when(registry.getSupplier(42)).thenReturn(Optional.of(comm));
        when(registry.getTimeSeriesId(42)).thenReturn(Optional.of(UUID.fromString("395c0232-d110-40fd-bd7f-2bb4a0f2009d")));
        when(registry.getSupplier(314)).thenReturn(Optional.empty());

        IecReadJob job = new IecReadJob(client, registry);

        // When / Then
        assertThatCode(job::run).doesNotThrowAnyException();
        verify(comm, times(1)).sendMeasurements(argThat(measurements ->
                measurements.size() == 2
                        && measurements.stream().allMatch(measurement ->
                        UUID.fromString("395c0232-d110-40fd-bd7f-2bb4a0f2009d").equals(measurement.getTimeSeriesId()))));
        verifyNoMoreInteractions(comm);
    }

    @Test
    void shouldContinueProcessingWhenOneSupplierFails() throws Exception {
        // Given
        IecClient client = mock(IecClient.class);
        DataPointRegistry registry = mock(DataPointRegistry.class);
        PegelHubCommunicator commFailing = mock(PegelHubCommunicator.class);
        PegelHubCommunicator commOk = mock(PegelHubCommunicator.class);

        Map<Integer, List<Measurement>> grouped = Map.of(
                10, List.of(m(10)),
                20, List.of(m(20))
        );

        when(client.drainGroupedMeasurements()).thenReturn(grouped);
        when(registry.getSupplier(10)).thenReturn(Optional.of(commFailing));
        when(registry.getSupplier(20)).thenReturn(Optional.of(commOk));
        when(registry.getTimeSeriesId(10)).thenReturn(Optional.of(UUID.fromString("395c0232-d110-40fd-bd7f-2bb4a0f2009d")));
        when(registry.getTimeSeriesId(20)).thenReturn(Optional.of(UUID.fromString("abdc0232-d110-40fd-bd7f-2bb4a0f2009d")));

        doThrow(new RuntimeException("exception")).when(commFailing).sendMeasurements(anyList());

        IecReadJob job = new IecReadJob(client, registry);

        // When
        job.run();

        // Then
        verify(commOk, times(1)).sendMeasurements(anyList());
    }
}
