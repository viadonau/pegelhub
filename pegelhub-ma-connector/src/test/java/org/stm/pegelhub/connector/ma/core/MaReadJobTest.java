package org.stm.pegelhub.connector.ma.core;

import com.stm.pegelhub.lib.PegelHubCommunicator;
import com.stm.pegelhub.lib.model.Measurement;
import org.junit.jupiter.api.Test;
import org.stm.pegelhub.connector.ma.jni.RevPiReader;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MaReadJobTest {

    @Test
    void shouldSendMeasurementPerOffsetAndCloseReader() {
        InputRegistry registry = mock(InputRegistry.class);
        RevPiReader reader = mock(RevPiReader.class);
        PegelHubCommunicator communicator = mock(PegelHubCommunicator.class);

        when(registry.supplierOffsets()).thenReturn(Set.of(10));
        when(reader.readFromOffset(10)).thenReturn(42);
        when(registry.getSupplier(10)).thenReturn(Optional.of(communicator));

        MaReadJob job = new MaReadJob(registry, reader);
        job.run();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Measurement>> captor = ArgumentCaptor.forClass(List.class);
        verify(communicator, times(1)).sendMeasurements(captor.capture());
        List<Measurement> list = captor.getValue();
        assertNotNull(list);
        assertEquals(1, list.size());

        verify(reader, times(1)).close();
    }

    @Test
    void shouldSkipWhenSupplierMissingAndContinue() {
        InputRegistry registry = mock(InputRegistry.class);
        RevPiReader reader = mock(RevPiReader.class);

        when(registry.supplierOffsets()).thenReturn(Set.of(5));
        when(reader.readFromOffset(5)).thenReturn(13);
        when(registry.getSupplier(5)).thenReturn(Optional.empty());

        MaReadJob job = new MaReadJob(registry, reader);
        job.run();

        verify(reader, times(1)).close();
        // No communicator interactions expected
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
