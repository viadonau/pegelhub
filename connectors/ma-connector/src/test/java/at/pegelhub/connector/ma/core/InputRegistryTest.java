package at.pegelhub.connector.ma.core;

import at.pegelhub.connector.ma.jni.RevPiReader;
import at.pegelhub.lib.config.MappingDirection;
import at.pegelhub.lib.PegelHubClient;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InputRegistryTest {
    private static final UUID TIME_SERIES_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TIME_SERIES_B = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void shouldLoadInputs() throws Exception {
        RevPiReader revPiReader = mock(RevPiReader.class);
        when(revPiReader.resolveOffsetByName("A")).thenReturn(10);
        when(revPiReader.resolveOffsetByName("B")).thenReturn(12);
        PegelHubClient client = mock(PegelHubClient.class);

        InputRegistry registry = new InputRegistry(revPiReader, List.of(
                mapping("A", TIME_SERIES_A),
                mapping("B", TIME_SERIES_B)), client);
        registry.loadInputs();

        Set<Integer> offsets = registry.protocolOffsets();
        assertEquals(Set.of(10, 12), offsets);

        Optional<PegelHubClient> offset10 = registry.getProtocolToCoreClient(10);
        assertTrue(offset10.isPresent());
        assertSame(client, offset10.get());
        assertEquals(Optional.of(TIME_SERIES_A), registry.getTimeSeriesId(10));

        registry.close();
        verify(client, times(1)).close();
    }

    @Test
    void shouldFailOnDuplicateRevInputs() throws Exception {
        RevPiReader revPiReader = mock(RevPiReader.class);
        when(revPiReader.resolveOffsetByName("X")).thenReturn(5);

        InputRegistry registry = new InputRegistry(revPiReader, List.of(
                mapping("X", TIME_SERIES_A),
                mapping("X", TIME_SERIES_B)), mock(PegelHubClient.class));

        assertThrows(IllegalArgumentException.class, registry::loadInputs);
    }

    @Test
    void shouldFailOnDuplicateResolvedOffsets() throws Exception {
        RevPiReader revPiReader = mock(RevPiReader.class);
        when(revPiReader.resolveOffsetByName("A")).thenReturn(7);
        when(revPiReader.resolveOffsetByName("B")).thenReturn(7);

        InputRegistry registry = new InputRegistry(revPiReader, List.of(
                mapping("A", TIME_SERIES_A),
                mapping("B", TIME_SERIES_B)), mock(PegelHubClient.class));

        assertThrows(IllegalArgumentException.class, registry::loadInputs);
    }

    @Test
    void shouldFailOnMissingDirection() {
        assertThrows(NullPointerException.class, () ->
                new InputMapping("A", TIME_SERIES_A, null));
    }

    @Test
    void shouldFailOnUnsupportedDirection() throws Exception {
        InputRegistry registry = new InputRegistry(mock(RevPiReader.class), List.of(
                new InputMapping("A", TIME_SERIES_A, MappingDirection.CORE_TO_EXTERNAL)), mock(PegelHubClient.class));

        assertThrows(IllegalArgumentException.class, registry::loadInputs);
    }

    @Test
    void shouldCloseClientWhenLoadInputsFailsBeforeRegistration() throws Exception {
        PegelHubClient client = mock(PegelHubClient.class);
        InputRegistry registry = new InputRegistry(mock(RevPiReader.class), List.of(
                new InputMapping("A", TIME_SERIES_A, MappingDirection.CORE_TO_EXTERNAL)), client);

        assertThrows(IllegalArgumentException.class, registry::loadInputs);

        registry.close();
        verify(client, times(1)).close();
    }

    @Test
    void shouldExposeUnmodifiableOffsetsView() throws Exception {
        RevPiReader revPiReader = mock(RevPiReader.class);
        when(revPiReader.resolveOffsetByName("A")).thenReturn(3);

        InputRegistry registry = new InputRegistry(revPiReader, List.of(mapping("A", TIME_SERIES_A)), mock(PegelHubClient.class));
        registry.loadInputs();

        Set<Integer> view = registry.protocolOffsets();
        assertThrows(UnsupportedOperationException.class, () -> view.add(99));
    }

    @Test
    void shouldReturnEmptyWhenOffsetUnknown() {
        InputRegistry registry = new InputRegistry(mock(RevPiReader.class), List.of(), mock(PegelHubClient.class));
        assertTrue(registry.getProtocolToCoreClient(42).isEmpty());
        assertTrue(registry.getTimeSeriesId(42).isEmpty());
    }

    private static InputMapping mapping(String revInput, UUID timeSeriesId) {
        return new InputMapping(revInput, timeSeriesId, MappingDirection.EXTERNAL_TO_CORE);
    }
}
