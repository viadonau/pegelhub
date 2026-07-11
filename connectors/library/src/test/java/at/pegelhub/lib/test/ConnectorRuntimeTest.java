package at.pegelhub.lib.runtime;
import org.junit.jupiter.api.Test;

import java.time.Duration;
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

        ConnectorRuntime runtime = ConnectorRuntime.builder("test connector")
                .fixedDelayTask("poll", () -> {
                    runs.incrementAndGet();
                    ran.countDown();
                }, Duration.ofMillis(10))
                .closeOnStop(() -> closed.set(true))
                .build();

        runtime.start();

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

        try (ConnectorRuntime runtime = ConnectorRuntime.builder("ordered connector")
                .onStart(() -> started.set(true))
                .fixedDelayTask("poll", () -> {
                    taskSawStart.set(started.get());
                    ran.countDown();
                }, Duration.ofMillis(10))
                .build()) {
            runtime.start();
            assertTrue(ran.await(2, TimeUnit.SECONDS));
        }

        assertTrue(taskSawStart.get());
    }

    @Test
    void startAndStopAreIdempotent() {
        AtomicInteger closes = new AtomicInteger();

        ConnectorRuntime runtime = ConnectorRuntime.builder("idempotent connector")
                .closeOnStop(closes::incrementAndGet)
                .build();

        runtime.start();
        runtime.start();
        runtime.stop();
        runtime.stop();

        assertEquals(1, closes.get());
    }

    @Test
    void startupFailureClosesRegisteredResources() {
        AtomicInteger closes = new AtomicInteger();

        ConnectorRuntime runtime = ConnectorRuntime.builder("failing connector")
                .closeOnStop(closes::incrementAndGet)
                .onStart(() -> {
                    throw new IllegalStateException("boom");
                })
                .build();

        assertThrows(RuntimeException.class, runtime::start);
        runtime.stop();

        assertEquals(1, closes.get());
    }
}
