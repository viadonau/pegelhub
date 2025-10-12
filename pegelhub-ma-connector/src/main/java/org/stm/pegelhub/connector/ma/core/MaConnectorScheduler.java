package org.stm.pegelhub.connector.ma.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public final class MaConnectorScheduler {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final MaReadJob readJob;
    private final Duration delay;

    /**
     * Starts periodic execution of the read job with the configured delay.
     */
    public void start() {
        scheduler.scheduleWithFixedDelay(
                readJob,
                1,
                delay.toSeconds(),
                TimeUnit.SECONDS
        );
    }


    /**
     * Stops the scheduler and waits briefly for termination.
     */
    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(15, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
        }

        log.info("mA ConnectorScheduler stopped");
    }
}
