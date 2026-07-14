package at.pegelhub.lib.runtime;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectorRuntimeTest {
    @Test
    void startsRealSchedulerAndRunsRegisteredTask() throws Exception {
        CountDownLatch ran = new CountDownLatch(1);
        AtomicInteger runs = new AtomicInteger();
        AtomicBoolean closed = new AtomicBoolean(false);

        ConnectorRuntimeAssembly assembly = ConnectorRuntimeAssembly.begin("test connector");
        assembly.fixedDelayTask("poll", () -> {
                    runs.incrementAndGet();
                    ran.countDown();
                }, Duration.ofMillis(10));
        assembly.own((AutoCloseable) () -> closed.set(true));
        ConnectorRuntimeDefinition definition = assembly.complete();

        ConnectorRuntime runtime = ConnectorRuntime.start(definition);

        assertTrue(ran.await(2, TimeUnit.SECONDS));
        runtime.stop();

        assertTrue(runs.get() >= 1);
        assertTrue(closed.get());
    }

    @Test
    void startHookRunsBeforeScheduledTasks() throws Exception {
        CountDownLatch ran = new CountDownLatch(1);
        AtomicBoolean started = new AtomicBoolean(false);
        AtomicBoolean taskSawStart = new AtomicBoolean(false);

        ConnectorRuntimeDefinition definition = ConnectorRuntimeAssembly.begin("ordered connector")
                .onStart(() -> started.set(true))
                .fixedDelayTask("poll", () -> {
                    taskSawStart.set(started.get());
                    ran.countDown();
                }, Duration.ofMillis(10))
                .complete();
        try (ConnectorRuntime runtime = ConnectorRuntime.start(definition)) {
            assertTrue(ran.await(2, TimeUnit.SECONDS));
        }

        assertTrue(taskSawStart.get());
    }

    @Test
    void startAndStopAreIdempotent() {
        AtomicInteger closes = new AtomicInteger();

        ConnectorRuntimeAssembly assembly = ConnectorRuntimeAssembly.begin("idempotent connector");
        assembly.own((AutoCloseable) closes::incrementAndGet);
        ConnectorRuntime runtime = ConnectorRuntime.start(assembly.complete());

        runtime.stop();
        runtime.stop();

        assertEquals(1, closes.get());
    }

    @Test
    void startupFailureClosesRegisteredResources() {
        AtomicInteger closes = new AtomicInteger();

        ConnectorRuntimeAssembly assembly = ConnectorRuntimeAssembly.begin("failing connector");
        assembly.own((AutoCloseable) closes::incrementAndGet);
        ConnectorRuntimeDefinition definition = assembly.onStart(() -> {
                    throw new IllegalStateException("boom");
                })
                .complete();

        assertThrows(RuntimeException.class, () -> ConnectorRuntime.start(definition));

        assertEquals(1, closes.get());
    }

    @Test
    void closesResourcesInReverseAcquisitionOrder() {
        List<String> closed = new ArrayList<>();
        ConnectorRuntimeAssembly assembly = ConnectorRuntimeAssembly.begin("ordered close connector");
        assembly.own((AutoCloseable) () -> closed.add("first"));
        assembly.own((AutoCloseable) () -> closed.add("second"));

        ConnectorRuntime runtime = ConnectorRuntime.start(assembly.complete());
        runtime.close();

        assertEquals(List.of("second", "first"), closed);
    }
}
