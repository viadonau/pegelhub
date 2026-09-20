package at.pegelhub.connector.iec.jobs;

import at.pegelhub.connector.iec.config.IecIngestionConfig.Mode;
import at.pegelhub.connector.iec.datapoints.IecMappingIndex;
import at.pegelhub.connector.iec.iec.IecClient;
import at.pegelhub.lib.PegelHubClient;
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

class IecToCoreJobTest {

    private static Measurement m(double v) {
        return new Measurement(null, Instant.now(), v);
    }

    @Test
    void shouldSendEachProtocolToCoreGroupAndIgnoreMissingOnes() throws Exception {
        // Given
        IecClient client = mock(IecClient.class);
        IecMappingIndex registry = mock(IecMappingIndex.class);
        PegelHubClient comm = mock(PegelHubClient.class);

        Map<Integer, List<Measurement>> grouped = Map.of(
                42, List.of(m(10), m(11)),
                314, List.of(m(3))
        );

        when(client.drainGroupedMeasurements()).thenReturn(grouped);
        when(registry.getTimeSeriesId(42)).thenReturn(Optional.of(UUID.fromString("395c0232-d110-40fd-bd7f-2bb4a0f2009d")));
        when(registry.getTimeSeriesId(314)).thenReturn(Optional.empty());

        IecToCoreJob job = new IecToCoreJob(client, registry, comm, Mode.ALL);

        // When / Then
        assertThatCode(job::run).doesNotThrowAnyException();
        verify(comm, times(1)).sendMeasurements(argThat(measurements ->
                measurements.size() == 2
                        && measurements.stream().allMatch(measurement ->
                        UUID.fromString("395c0232-d110-40fd-bd7f-2bb4a0f2009d").equals(measurement.getTimeSeriesId()))));
        verifyNoMoreInteractions(comm);
    }

    @Test
    void shouldContinueProcessingWhenOneProtocolToCoreSendFails() throws Exception {
        // Given
        IecClient client = mock(IecClient.class);
        IecMappingIndex registry = mock(IecMappingIndex.class);
        PegelHubClient comm = mock(PegelHubClient.class);

        Map<Integer, List<Measurement>> grouped = Map.of(
                10, List.of(m(10)),
                20, List.of(m(20))
        );

        when(client.drainGroupedMeasurements()).thenReturn(grouped);
        when(registry.getTimeSeriesId(10)).thenReturn(Optional.of(UUID.fromString("395c0232-d110-40fd-bd7f-2bb4a0f2009d")));
        when(registry.getTimeSeriesId(20)).thenReturn(Optional.of(UUID.fromString("abdc0232-d110-40fd-bd7f-2bb4a0f2009d")));

        doThrow(new RuntimeException("exception")).doNothing().when(comm).sendMeasurements(anyList());

        IecToCoreJob job = new IecToCoreJob(client, registry, comm, Mode.ALL);

        // When
        job.run();

        // Then
        verify(comm, times(2)).sendMeasurements(anyList());
    }

    @Test
    void shouldRetryABatchWhenCoreTemporarilyRejectsIt() throws Exception {
        IecClient client = mock(IecClient.class);
        IecMappingIndex registry = mock(IecMappingIndex.class);
        PegelHubClient comm = mock(PegelHubClient.class);
        UUID timeSeriesId = UUID.fromString("395c0232-d110-40fd-bd7f-2bb4a0f2009d");

        when(client.drainGroupedMeasurements())
                .thenReturn(Map.of(42, List.of(m(10))))
                .thenReturn(Map.of());
        when(registry.getTimeSeriesId(42)).thenReturn(Optional.of(timeSeriesId));
        doThrow(new RuntimeException("Core unavailable"))
                .doNothing()
                .when(comm).sendMeasurements(anyList());

        IecToCoreJob job = new IecToCoreJob(client, registry, comm, Mode.ALL);
        job.run();
        job.run();

        verify(comm, times(2)).sendMeasurements(argThat(measurements ->
                measurements.size() == 1 && timeSeriesId.equals(measurements.getFirst().getTimeSeriesId())));
    }

    @Test
    void latestSelectsOneReceivedValuePerIoaPreservingTimestampAndSkipsEmptyIntervals() throws Exception {
        var client = mock(IecClient.class);
        var registry = mock(IecMappingIndex.class);
        var core = mock(PegelHubClient.class);
        var firstId = UUID.randomUUID();
        var secondId = UUID.randomUUID();
        var at = Instant.parse("2026-09-20T10:04:59.123Z");
        when(registry.getTimeSeriesId(10)).thenReturn(Optional.of(firstId));
        when(registry.getTimeSeriesId(20)).thenReturn(Optional.of(secondId));
        when(client.drainGroupedMeasurements()).thenReturn(Map.of(
                10, List.of(new Measurement(null, at.minusSeconds(2), 10.0),
                        new Measurement(null, at, 11.0)),
                20, List.of(new Measurement(null, at, 20.0)),
                30, List.of())).thenReturn(Map.of());
        var job = new IecToCoreJob(client, registry, core, Mode.LATEST);

        job.run();
        job.run();

        verify(core).sendMeasurements(argThat(values -> values.size() == 1
                && values.getFirst().getTimeSeriesId().equals(firstId)
                && values.getFirst().getObservedAt().equals(at)
                && values.getFirst().getValue().equals(11.0)));
        verify(core).sendMeasurements(argThat(values -> values.size() == 1
                && values.getFirst().getTimeSeriesId().equals(secondId)
                && values.getFirst().getObservedAt().equals(at)
                && values.getFirst().getValue().equals(20.0)));
        verifyNoMoreInteractions(core);
        verify(registry, never()).getTimeSeriesId(30);
    }

    @Test
    void latestRetainsFailedSnapshotsAcrossIntervalsIncludingUnchangedValues() throws Exception {
        var client = mock(IecClient.class);
        var registry = mock(IecMappingIndex.class);
        var core = mock(PegelHubClient.class);
        var id = UUID.randomUUID();
        var at = Instant.parse("2026-09-20T10:04:59Z");
        when(registry.getTimeSeriesId(10)).thenReturn(Optional.of(id));
        when(client.drainGroupedMeasurements())
                .thenReturn(Map.of(10, List.of(new Measurement(null, at.minusSeconds(1), 9.0),
                        new Measurement(null, at, 10.0))))
                .thenReturn(Map.of(10, List.of(new Measurement(null, at.plusSeconds(299), 11.0),
                        new Measurement(null, at.plusSeconds(300), 10.0))))
                .thenReturn(Map.of());
        doThrow(new RuntimeException("Core unavailable")).doNothing().when(core).sendMeasurements(anyList());
        var job = new IecToCoreJob(client, registry, core, Mode.LATEST);

        job.run();
        job.run();
        job.run();

        var calls = inOrder(core);
        calls.verify(core).sendMeasurements(argThat(values -> values.size() == 1
                && values.getFirst().getObservedAt().equals(at)));
        calls.verify(core).sendMeasurements(argThat(values -> values.size() == 2
                && values.getFirst().getObservedAt().equals(at)
                && values.getLast().getObservedAt().equals(at.plusSeconds(300))
                && values.stream().allMatch(value -> value.getValue().equals(10.0)
                    && value.getTimeSeriesId().equals(id))));
        calls.verifyNoMoreInteractions();
    }
}
