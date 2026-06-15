package at.pegelhub.connector.iec.jobs;

import at.pegelhub.connector.iec.datapoints.DataPointRegistry;
import at.pegelhub.connector.iec.iec.IecClient;
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
                            communicator -> dataPointRegistry.getTimeSeriesId(ioa).ifPresentOrElse(
                                    timeSeriesId -> communicator.getLatestMeasurementOfTimeSeries(timeSeriesId)
                                            .ifPresentOrElse(
                                                    latest -> iecClient.sendMeasurement(ioa, latest),
                                                    () -> log.info("No measurement found for TimeSeries of IOA: {}.", ioa)
                                            ),
                                    () -> log.info("No TimeSeries ID configured for IOA: {}.", ioa)),
                            () -> log.info("No communicator found for ioa: {}.", ioa)
                    )
            );
        } catch (Exception e) {
            log.info("Error sending measurements: {}", e.getMessage());
        }
    }
}
