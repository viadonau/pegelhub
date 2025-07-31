package com.stm.pegelhub.connector.iec.datapoints;

import com.stm.pegelhub.lib.PegelHubCommunicator;
import com.stm.pegelhub.lib.PegelHubCommunicatorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DataPointRegistryTest {

    @TempDir
    Path dir;

    private void writeYaml(Path file, int ioa, boolean isSupplier) throws Exception {
        Files.writeString(file, """
                iecIOA: %d
                isSupplier: %s
                """.formatted(ioa, isSupplier));
    }

    @Test
    void shouldLoadSuppliersAndTakersAndIgnoreNonYamlFiles() throws Exception {
        // Given
        writeYaml(dir.resolve("s1.yaml"), 1001, true);
        writeYaml(dir.resolve("t1.yml"), 2002, false);
        Files.writeString(dir.resolve("readme.txt"), "ignore me");

        PegelHubCommunicator mockComm = mock(PegelHubCommunicator.class);

        try (MockedStatic<PegelHubCommunicatorFactory> mf =
                     mockStatic(PegelHubCommunicatorFactory.class)) {
            mf.when(() -> PegelHubCommunicatorFactory.create(any(URL.class), anyString()))
                    .thenReturn(mockComm);

            // When
            DataPointRegistry reg = new DataPointRegistry(
                    dir.toString(), new URL("http", "core.local", 8080, "/"));

            // Then
            assertThat(reg.supplierIoas()).containsExactly(1001);
            assertThat(reg.takerIoas()).containsExactly(2002);

            assertThat(reg.getSupplier(1001)).isPresent();
            assertThat(reg.getTaker(2002)).isPresent();
            assertThat(reg.getSupplier(9999)).isEmpty();
        }
    }

    @Test
    void shouldSkipDuplicateIoasAndKeepFirstEntry() throws Exception {
        // Given
        writeYaml(dir.resolve("a.yaml"), 1234, true);
        writeYaml(dir.resolve("b.yaml"), 1234, true); // duplicate

        PegelHubCommunicator mockComm = mock(PegelHubCommunicator.class);

        try (MockedStatic<PegelHubCommunicatorFactory> mf =
                     mockStatic(PegelHubCommunicatorFactory.class)) {
            mf.when(() -> PegelHubCommunicatorFactory.create(any(URL.class), anyString()))
                    .thenReturn(mockComm);

            // When
            DataPointRegistry reg = new DataPointRegistry(
                    dir.toString(), new URL("http", "core.local", 8080, "/"));

            // Then
            Set<Integer> suppliers = reg.supplierIoas();
            assertThat(suppliers).containsExactly(1234);
        }
    }
}
