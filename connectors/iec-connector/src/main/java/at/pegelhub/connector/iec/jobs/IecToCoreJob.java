package at.pegelhub.connector.iec.jobs;

import at.pegelhub.connector.iec.datapoints.IecMappingIndex;
import at.pegelhub.connector.iec.iec.IecClient;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.model.Measurement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class IecToCoreJob implements Runnable {
    private final IecClient iecClient;
    private final IecMappingIndex mappingIndex;
    private final PegelHubClient coreClient;

    @Override
    public void run() {
        try {
            iecClient.drainGroupedMeasurements().forEach((ioa, measurements) ->
                    sendMeasurements(ioa, measurements)
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("Processing service was interrupted and is shutting down.");
        } catch (Exception e) {
            log.error("An error occurred during event processing. Skipping batch.", e);
        }
    }

    private void sendMeasurements(int ioa, List<Measurement> measurements) {
        try {
            UUID timeSeriesId = mappingIndex.getTimeSeriesId(ioa)
                    .orElseThrow(() -> new IllegalStateException("Missing TimeSeries ID for IOA " + ioa));
            coreClient.sendMeasurements(toTimeSeriesMeasurements(timeSeriesId, measurements));
            log.info("Sent {} measurements for IOA {}.", measurements.size(), ioa);
        } catch (Exception ex) {
            log.error("Failed sending measurements for IOA {}.", ioa, ex);
        }
    }

    private List<Measurement> toTimeSeriesMeasurements(UUID timeSeriesId, List<Measurement> measurements) {
        return measurements.stream()
                .map(measurement -> new Measurement(timeSeriesId, measurement.getObservedAt(), measurement.getValue()))
                .toList();
    }
}
