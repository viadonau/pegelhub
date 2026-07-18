package at.pegelhub.connector.iec.jobs;

import at.pegelhub.connector.iec.datapoints.IecMappingIndex;
import at.pegelhub.connector.iec.iec.IecClient;
import at.pegelhub.lib.PegelHubClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class CoreToIecJob implements Runnable {
    private final IecClient iecClient;
    private final IecMappingIndex mappingIndex;
    private final PegelHubClient coreClient;

    @Override
    public void run() {
        try {
            mappingIndex.coreToProtocolIoas().forEach(ioa ->
                    mappingIndex.getTimeSeriesId(ioa).ifPresentOrElse(
                            timeSeriesId -> coreClient.getLatestMeasurementOfTimeSeries(timeSeriesId)
                                    .ifPresentOrElse(
                                            latest -> iecClient.sendMeasurement(ioa, latest),
                                            () -> log.info("No measurement found for TimeSeries of IOA: {}.", ioa)),
                            () -> log.info("No TimeSeries ID configured for IOA: {}.", ioa))
            );
        } catch (Exception e) {
            log.info("Error sending measurements: {}", e.getMessage());
        }
    }
}
