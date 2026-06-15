package at.pegelhub.connector.iec.jobs;

import at.pegelhub.connector.iec.datapoints.DataPointRegistry;
import at.pegelhub.connector.iec.iec.IecClient;
import at.pegelhub.lib.model.Measurement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class IecReadJob implements Runnable {
    private final IecClient iecClient;
    private final DataPointRegistry dataPointRegistry;

    @Override
    public void run() {
        try {
            iecClient.drainGroupedMeasurements().forEach((ioa, measurements) ->
                    dataPointRegistry.getSupplier(ioa).ifPresentOrElse(
                            communicator -> {
                                try {
                                    UUID timeSeriesId = dataPointRegistry.getTimeSeriesId(ioa)
                                            .orElseThrow(() -> new IllegalStateException("Missing TimeSeries ID for IOA " + ioa));
                                    communicator.sendMeasurements(toTimeSeriesMeasurements(timeSeriesId, measurements));
                                    log.info("Sent {} measurements for IOA {}.", measurements.size(), ioa);
                                } catch (Exception ex) {
                                    log.error("Failed sending measurements for IOA {}.", ioa, ex);
                                }
                            },
                            () -> log.error("Missing communicator for IOA {} at send time (unexpected).", ioa)
                    )
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("Processing service was interrupted and is shutting down.");
        } catch (Exception e) {
            log.error("An error occurred during event processing. Skipping batch.", e);
        }
    }

    private List<Measurement> toTimeSeriesMeasurements(UUID timeSeriesId, List<Measurement> measurements) {
        return measurements.stream()
                .map(measurement -> new Measurement(timeSeriesId, measurement.getObservedAt(), measurement.getValue()))
                .toList();
    }
}
