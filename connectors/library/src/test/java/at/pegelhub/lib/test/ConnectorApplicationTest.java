package at.pegelhub.lib.test;

import at.pegelhub.lib.PegelHubClientFactory;
import at.pegelhub.lib.config.ConnectorConfigDirectory;
import at.pegelhub.lib.runtime.ConnectorApplication;
import at.pegelhub.lib.runtime.ConnectorModule;
import at.pegelhub.lib.runtime.ConnectorRuntime;
import at.pegelhub.lib.runtime.ConnectorRuntimeAssembly;
import at.pegelhub.lib.runtime.ConnectorRuntimeDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectorApplicationTest {
    @TempDir
    Path configDir;

    @Test
    void startResolvesConfigDirAndRunsRealScheduledTask() throws Exception {
        CountDownLatch ran = new CountDownLatch(1);
        AtomicReference<Path> seenConfigDir = new AtomicReference<>();

        ConnectorModule module = new ConnectorModule() {
            @Override
            public String name() {
                return "test connector";
            }

            @Override
            public ConnectorRuntimeDefinition define(
                    ConnectorConfigDirectory configDirectory,
                    PegelHubClientFactory coreClients) {
                seenConfigDir.set(configDirectory.path());
                try (ConnectorRuntimeAssembly assembly = ConnectorRuntimeAssembly.begin(name())) {
                    return assembly.fixedDelayTask("poll", ran::countDown, Duration.ofMillis(10)).complete();
                }
            }
        };

        try (ConnectorRuntime runtime = ConnectorApplication.start(new String[]{configDir.toString()}, module)) {
            assertTrue(ran.await(2, TimeUnit.SECONDS));
        }

        assertEquals(configDir, seenConfigDir.get());
    }

    @Test
    void startUsesDefaultConfigDirectoryWhenArgsAreEmpty() throws Exception {
        AtomicReference<Path> seenConfigDir = new AtomicReference<>();
        ConnectorModule module = moduleCapturingConfigDirectory(seenConfigDir);

        try (ConnectorRuntime ignored = ConnectorApplication.start(new String[0], module)) {
            assertEquals(Path.of("/app/config"), seenConfigDir.get());
        }
    }

    @Test
    void startPropagatesModuleStartupFailure() {
        AtomicBoolean closed = new AtomicBoolean(false);
        ConnectorModule module = new ConnectorModule() {
            @Override
            public String name() {
                return "failing connector";
            }

            @Override
            public ConnectorRuntimeDefinition define(
                    ConnectorConfigDirectory configDirectory,
                    PegelHubClientFactory coreClients) {
                try (ConnectorRuntimeAssembly assembly = ConnectorRuntimeAssembly.begin(name())) {
                    assembly.onStart(() -> {
                            throw new IllegalStateException("boom");
                        });
                    assembly.own((AutoCloseable) () -> closed.set(true));
                    return assembly.complete();
                }
            }
        };

        assertThrows(RuntimeException.class, () -> ConnectorApplication.start(new String[]{configDir.toString()}, module));
        assertTrue(closed.get());
    }

    private static ConnectorModule moduleCapturingConfigDirectory(AtomicReference<Path> seenConfigDir) {
        return new ConnectorModule() {
            @Override
            public String name() {
                return "test connector";
            }

            @Override
            public ConnectorRuntimeDefinition define(
                    ConnectorConfigDirectory configDirectory,
                    PegelHubClientFactory coreClients) {
                seenConfigDir.set(configDirectory.path());
                return ConnectorRuntimeAssembly.begin(name()).complete();
            }
        };
    }
}
