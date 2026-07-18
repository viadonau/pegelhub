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

class MaInputMappingIndexTest {
    private static final UUID TIME_SERIES_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TIME_SERIES_B = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void shouldLoadInputs() throws Exception {
        RevPiReader revPiReader = mock(RevPiReader.class);
        when(revPiReader.resolveOffsetByName("A")).thenReturn(10);
        when(revPiReader.resolveOffsetByName("B")).thenReturn(12);
        MaInputMappingIndex registry = new MaInputMappingIndex(revPiReader, List.of(
                mapping("A", TIME_SERIES_A),
                mapping("B", TIME_SERIES_B)));
        registry.loadInputs();

        Set<Integer> offsets = registry.protocolOffsets();
        assertEquals(Set.of(10, 12), offsets);

        assertEquals(Optional.of(TIME_SERIES_A), registry.getTimeSeriesId(10));
    }

    @Test
    void shouldFailOnDuplicateRevInputs() throws Exception {
        RevPiReader revPiReader = mock(RevPiReader.class);
        when(revPiReader.resolveOffsetByName("X")).thenReturn(5);

        MaInputMappingIndex registry = new MaInputMappingIndex(revPiReader, List.of(
                mapping("X", TIME_SERIES_A),
                mapping("X", TIME_SERIES_B)));

        assertThrows(IllegalArgumentException.class, registry::loadInputs);
    }

    @Test
    void shouldFailOnDuplicateResolvedOffsets() throws Exception {
        RevPiReader revPiReader = mock(RevPiReader.class);
        when(revPiReader.resolveOffsetByName("A")).thenReturn(7);
        when(revPiReader.resolveOffsetByName("B")).thenReturn(7);

        MaInputMappingIndex registry = new MaInputMappingIndex(revPiReader, List.of(
                mapping("A", TIME_SERIES_A),
                mapping("B", TIME_SERIES_B)));

        assertThrows(IllegalArgumentException.class, registry::loadInputs);
    }

    @Test
    void shouldFailOnMissingDirection() {
        assertThrows(NullPointerException.class, () ->
                new InputMapping("A", TIME_SERIES_A, null));
    }

    @Test
    void shouldFailOnUnsupportedDirection() throws Exception {
        MaInputMappingIndex registry = new MaInputMappingIndex(mock(RevPiReader.class), List.of(
                new InputMapping("A", TIME_SERIES_A, MappingDirection.CORE_TO_EXTERNAL)));

        assertThrows(IllegalArgumentException.class, registry::loadInputs);
    }

    @Test
    void shouldKeepOwnershipOutsideMappingIndex() {
        MaInputMappingIndex registry = new MaInputMappingIndex(mock(RevPiReader.class), List.of(
                new InputMapping("A", TIME_SERIES_A, MappingDirection.CORE_TO_EXTERNAL)));

        assertThrows(IllegalArgumentException.class, registry::loadInputs);

        assertTrue(registry.protocolOffsets().isEmpty());
    }

    @Test
    void shouldExposeUnmodifiableOffsetsView() throws Exception {
        RevPiReader revPiReader = mock(RevPiReader.class);
        when(revPiReader.resolveOffsetByName("A")).thenReturn(3);

        MaInputMappingIndex registry = new MaInputMappingIndex(revPiReader, List.of(mapping("A", TIME_SERIES_A)));
        registry.loadInputs();

        Set<Integer> view = registry.protocolOffsets();
        assertThrows(UnsupportedOperationException.class, () -> view.add(99));
    }

    @Test
    void shouldReturnEmptyWhenOffsetUnknown() {
        MaInputMappingIndex registry = new MaInputMappingIndex(mock(RevPiReader.class), List.of());
        assertTrue(registry.getTimeSeriesId(42).isEmpty());
    }

    private static InputMapping mapping(String revInput, UUID timeSeriesId) {
        return new InputMapping(revInput, timeSeriesId, MappingDirection.EXTERNAL_TO_CORE);
    }
}
