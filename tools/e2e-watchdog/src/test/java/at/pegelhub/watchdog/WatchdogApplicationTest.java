package at.pegelhub.watchdog;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class WatchdogApplicationTest {
    @Test
    void completedCyclesHeartbeatOnlyEveryThirtySeconds() {
        var nanoTime = new AtomicLong();
        var heartbeats = new ArrayList<Long>();
        var task = WatchdogApplication.withHeartbeat(() -> {},
                () -> heartbeats.add(TimeUnit.NANOSECONDS.toSeconds(nanoTime.get())), nanoTime::get);

        for (int second = 0; second <= 60; second++) {
            nanoTime.set(TimeUnit.SECONDS.toNanos(second));
            task.run();
        }

        assertEquals(List.of(30L, 60L), heartbeats);
    }

    @Test
    void slowWorkOnlyRecordsProgressAfterCompletionAndFailuresPropagate() {
        var nanoTime = new AtomicLong();
        var attempts = new AtomicInteger();
        var heartbeats = new ArrayList<Long>();
        var failure = new IllegalStateException("cycle failed");
        var task = WatchdogApplication.withHeartbeat(() -> {
            nanoTime.addAndGet(TimeUnit.SECONDS.toNanos(35));
            if (attempts.incrementAndGet() == 1) {
                throw failure;
            }
        }, () -> heartbeats.add(nanoTime.get()), nanoTime::get);

        assertSame(failure, assertThrows(IllegalStateException.class, task::run));
        assertTrue(heartbeats.isEmpty());
        task.run();
        assertEquals(List.of(TimeUnit.SECONDS.toNanos(70)), heartbeats);
    }

    @Test
    void shutdownWakesThePollDelay() throws Exception {
        var completed = new CountDownLatch(1);
        var stop = new CountDownLatch(1);
        try (var executor = Executors.newSingleThreadExecutor()) {
            var task = executor.submit(() -> {
                WatchdogApplication.runLoop(completed::countDown, 60, stop);
                return null;
            });
            try {
                assertTrue(completed.await(2, TimeUnit.SECONDS));
            } finally {
                stop.countDown();
            }
            task.get(2, TimeUnit.SECONDS);
        }
    }
}
