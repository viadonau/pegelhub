package at.pegelhub.connector.ma.core;

import at.pegelhub.lib.PegelHubCommunicator;
import at.pegelhub.lib.model.Measurement;
import org.junit.jupiter.api.Test;
import at.pegelhub.connector.ma.jni.RevPiReader;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MaReadJobTest {
    private static final UUID TIME_SERIES_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void shouldSendMeasurementPerOffsetAndCloseReader() {
        InputRegistry registry = mock(InputRegistry.class);
        RevPiReader reader = mock(RevPiReader.class);
        PegelHubCommunicator communicator = mock(PegelHubCommunicator.class);

        when(registry.supplierOffsets()).thenReturn(Set.of(10));
        when(reader.readFromOffset(10)).thenReturn(42);
        when(registry.getTimeSeriesId(10)).thenReturn(Optional.of(TIME_SERIES_ID));
        when(registry.getSupplier(10)).thenReturn(Optional.of(communicator));

        MaReadJob job = new MaReadJob(registry, reader);
        job.run();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Measurement>> captor = ArgumentCaptor.forClass(List.class);
        verify(communicator, times(1)).sendMeasurements(captor.capture());
        List<Measurement> list = captor.getValue();
        assertNotNull(list);
        assertEquals(1, list.size());
        Measurement measurement = list.getFirst();
        assertEquals(TIME_SERIES_ID, measurement.getTimeSeriesId());
        assertEquals(42.0, measurement.getValue());
        assertNotNull(measurement.getObservedAt());

        verify(reader, times(1)).close();
    }

    @Test
    void shouldSkipWhenSupplierMissingAndContinue() {
        InputRegistry registry = mock(InputRegistry.class);
        RevPiReader reader = mock(RevPiReader.class);

        when(registry.supplierOffsets()).thenReturn(Set.of(5));
        when(reader.readFromOffset(5)).thenReturn(13);
        when(registry.getTimeSeriesId(5)).thenReturn(Optional.of(TIME_SERIES_ID));
        when(registry.getSupplier(5)).thenReturn(Optional.empty());

        MaReadJob job = new MaReadJob(registry, reader);
        job.run();

        verify(reader, times(1)).close();
        // No communicator interactions expected
    }

    @Test
    void shouldSkipWhenTimeSeriesIdMissingAndContinue() {
        InputRegistry registry = mock(InputRegistry.class);
        RevPiReader reader = mock(RevPiReader.class);

        when(registry.supplierOffsets()).thenReturn(Set.of(5));
        when(reader.readFromOffset(5)).thenReturn(13);
        when(registry.getTimeSeriesId(5)).thenReturn(Optional.empty());

        MaReadJob job = new MaReadJob(registry, reader);
        job.run();

        verify(registry, never()).getSupplier(5);
        verify(reader, times(1)).close();
    }

    @Test
    void shouldSkipOffsetOnReadErrorAndCloseReader() {
        InputRegistry registry = mock(InputRegistry.class);
        RevPiReader reader = mock(RevPiReader.class);

        when(registry.supplierOffsets()).thenReturn(Set.of(9));
        when(reader.readFromOffset(9)).thenThrow(new RuntimeException("boom"));

        MaReadJob job = new MaReadJob(registry, reader);
        job.run();

        verify(reader, times(1)).close();
        // No communicator interactions expected
    }
}
