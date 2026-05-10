package at.pegelhub.connector.ma.core;

import at.pegelhub.lib.PegelHubCommunicator;
import at.pegelhub.lib.PegelHubCommunicatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import at.pegelhub.connector.ma.jni.RevPiReader;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InputRegistryTest {

    @TempDir
    Path tmp;

    RevPiReader revPiReader;
    MockedStatic<PegelHubCommunicatorFactory> factoryMock;

    URL coreUrl;

    @BeforeEach
    void setUp() throws Exception {
        revPiReader = mock(RevPiReader.class);
        coreUrl = new URL("http", "localhost", 8080, "/");
        factoryMock = mockStatic(PegelHubCommunicatorFactory.class);
    }

    @AfterEach
    void tearDown() {
        if (factoryMock != null) factoryMock.close();
    }

    private void writeYaml(String fileName, String content) throws Exception {
        Path p = tmp.resolve(fileName);
        Files.writeString(p, content);
    }

    @Test
    void shouldLoadInputsAndIgnoreNonYamlFiles() throws Exception {
        writeYaml("a.yaml", "revInput: A\n");
        writeYaml("b.yml",  "revInput: B\n");
        writeYaml("ignore.txt", "revInput: IGNORED\n");

        when(revPiReader.resolveOffsetByName("A")).thenReturn(10);
        when(revPiReader.resolveOffsetByName("B")).thenReturn(12);

        PegelHubCommunicator commA = mock(PegelHubCommunicator.class);
        PegelHubCommunicator commB = mock(PegelHubCommunicator.class);

        factoryMock.when(() -> PegelHubCommunicatorFactory.create(eq(coreUrl), argThat(p -> p.endsWith("a.yaml"))))
                .thenReturn(commA);
        factoryMock.when(() -> PegelHubCommunicatorFactory.create(eq(coreUrl), argThat(p -> p.endsWith("b.yml"))))
                .thenReturn(commB);

        InputRegistry registry = new InputRegistry(revPiReader, tmp.toString(), coreUrl);
        registry.loadInputs();

        Set<Integer> offsets = registry.supplierOffsets();
        assertEquals(Set.of(10, 12), offsets);

        Optional<PegelHubCommunicator> s10 = registry.getSupplier(10);
        assertTrue(s10.isPresent());
        assertSame(commA, s10.get());
    }

    @Test
    void shouldSkipDuplicateRevInputsAndKeepFirst() throws Exception {
        writeYaml("x1.yaml", "revInput: X\n");
        writeYaml("x2.yaml", "revInput: X\n");

        when(revPiReader.resolveOffsetByName("X")).thenReturn(5);
        PegelHubCommunicator comm = mock(PegelHubCommunicator.class);
        factoryMock.when(() -> PegelHubCommunicatorFactory.create(eq(coreUrl), anyString()))
                .thenReturn(comm);

        InputRegistry registry = new InputRegistry(revPiReader, tmp.toString(), coreUrl);
        registry.loadInputs();

        assertEquals(1, registry.supplierOffsets().size());
    }

    @Test
    void shouldSkipDuplicateResolvedOffsetsAndKeepFirst() throws Exception {
        writeYaml("a.yaml", "revInput: A\n");
        writeYaml("b.yaml", "revInput: B\n");

        when(revPiReader.resolveOffsetByName("A")).thenReturn(7);
        when(revPiReader.resolveOffsetByName("B")).thenReturn(7); // same resolved offset

        PegelHubCommunicator comm = mock(PegelHubCommunicator.class);
        factoryMock.when(() -> PegelHubCommunicatorFactory.create(eq(coreUrl), anyString()))
                .thenReturn(comm);

        InputRegistry registry = new InputRegistry(revPiReader, tmp.toString(), coreUrl);
        registry.loadInputs();

        assertEquals(Set.of(7), registry.supplierOffsets());
    }

    @Test
    void shouldSkipFilesMissingRevInput() throws Exception {
        writeYaml("bad.yaml", "apiToken: 123\n"); // no revInput

        InputRegistry registry = new InputRegistry(revPiReader, tmp.toString(), coreUrl);
        registry.loadInputs();

        assertTrue(registry.supplierOffsets().isEmpty());
    }

    @Test
    void shouldExposeUnmodifiableOffsetsView() throws Exception {
        writeYaml("a.yaml", "revInput: A\n");
        when(revPiReader.resolveOffsetByName("A")).thenReturn(3);
        PegelHubCommunicator comm = mock(PegelHubCommunicator.class);
        factoryMock.when(() -> PegelHubCommunicatorFactory.create(eq(coreUrl), anyString())).thenReturn(comm);

        InputRegistry registry = new InputRegistry(revPiReader, tmp.toString(), coreUrl);
        registry.loadInputs();

        Set<Integer> view = registry.supplierOffsets();
        assertThrows(UnsupportedOperationException.class, () -> view.add(99));
    }

    @Test
    void shouldReturnEmptyWhenSupplierUnknown() {
        InputRegistry registry = new InputRegistry(revPiReader, tmp.toString(), coreUrl);
        assertTrue(registry.getSupplier(42).isEmpty());
    }
}
