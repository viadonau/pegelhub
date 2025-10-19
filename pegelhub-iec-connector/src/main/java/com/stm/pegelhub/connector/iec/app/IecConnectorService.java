package com.stm.pegelhub.connector.iec.app;

import com.stm.pegelhub.connector.iec.datapoints.DataPointRegistry;
import com.stm.pegelhub.connector.iec.iec.IecClient;
import com.stm.pegelhub.connector.iec.jobs.IecReadJob;
import com.stm.pegelhub.connector.iec.jobs.IecWriteJob;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@AllArgsConstructor
public class IecConnectorService {
    private final Duration delay;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final IecClient iecClient;
    private final DataPointRegistry dataPointRegistry;

    public void start() {
        iecClient.connect();

        var readStrategy = new IecReadJob(iecClient, dataPointRegistry);
        var writeStrategy = new IecWriteJob(iecClient, dataPointRegistry);

        scheduler.scheduleWithFixedDelay(
                readStrategy,
                1,
                delay.toSeconds(),
                TimeUnit.SECONDS
        );

        scheduler.scheduleWithFixedDelay(
                writeStrategy,
                1,
                delay.toSeconds(),
                TimeUnit.SECONDS
        );

        log.info("IEC Connector Service started successfully");
    }

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
        iecClient.disconnect();
        log.info("IEC Connector Service stopped");
    }
}
