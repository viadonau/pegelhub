package com.stm.pegelhub.connector.iec.jobs;

import com.stm.pegelhub.connector.iec.datapoints.DataPointRegistry;
import com.stm.pegelhub.connector.iec.iec.IecClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class IecWriteJob implements Runnable {
    private final IecClient iecClient;
    private final DataPointRegistry dataPointRegistry;

    @Override
    public void run() {
        try {
            dataPointRegistry.takerIoas().forEach(ioa ->
                    dataPointRegistry.getTaker(ioa).ifPresentOrElse(
                            communicator -> communicator.getLatestMeasurementOfStation()
                                    .ifPresentOrElse(
                                            latest -> iecClient.sendMeasurement(ioa, latest),
                                            () -> log.info("No measurement found for station of ioa: {}.", ioa)
                                    ),
                            () -> log.info("No communicator found for ioa: {}.", ioa)
                    )
            );
        } catch (Exception e) {
            log.info("Error sending measurements: {}", e.getMessage());
        }
    }
}
