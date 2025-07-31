package com.stm.pegelhub.connector.iec.jobs;

import com.stm.pegelhub.connector.iec.datapoints.DataPointRegistry;
import com.stm.pegelhub.connector.iec.iec.IecClient;
import com.stm.pegelhub.lib.PegelHubCommunicator;
import com.stm.pegelhub.lib.model.Measurement;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThatCode;

class IecReadJobTest {

    private static Measurement m(double v) {
        var fields = new HashMap<String, Double>();
        fields.put("value", v);
        return new Measurement(LocalDateTime.now(ZoneOffset.UTC), fields, new HashMap<>());
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
        when(registry.getSupplier(314)).thenReturn(Optional.empty());

        IecReadJob job = new IecReadJob(client, registry);

        // When / Then
        assertThatCode(job::run).doesNotThrowAnyException();
        verify(comm, times(1)).sendMeasurements(grouped.get(42));
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

        doThrow(new RuntimeException("exception")).when(commFailing).sendMeasurements(anyList());

        IecReadJob job = new IecReadJob(client, registry);

        // When
        job.run();

        // Then
        verify(commOk, times(1)).sendMeasurements(grouped.get(20));
    }
}
