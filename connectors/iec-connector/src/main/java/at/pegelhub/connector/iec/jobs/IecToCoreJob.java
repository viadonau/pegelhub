package at.pegelhub.connector.iec.jobs;

import at.pegelhub.connector.iec.datapoints.IecMappingIndex;
import at.pegelhub.connector.iec.iec.IecClient;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.model.Measurement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class IecToCoreJob implements Runnable {
    private final IecClient iecClient;
    private final IecMappingIndex mappingIndex;
    private final PegelHubClient coreClient;
    private final Map<Integer, List<Measurement>> pendingMeasurements = new LinkedHashMap<>();

    @Override
    public void run() {
        try {
            iecClient.drainGroupedMeasurements().forEach((ioa, measurements) ->
                    pendingMeasurements.computeIfAbsent(ioa, ignored -> new ArrayList<>()).addAll(measurements));

            Iterator<Map.Entry<Integer, List<Measurement>>> pending = pendingMeasurements.entrySet().iterator();
            while (pending.hasNext()) {
                Map.Entry<Integer, List<Measurement>> batch = pending.next();
                if (sendMeasurements(batch.getKey(), batch.getValue())) {
                    pending.remove();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("Processing service was interrupted and is shutting down.");
        } catch (Exception e) {
            log.error("An error occurred during event processing. Retaining pending batches.", e);
        }
    }

    private boolean sendMeasurements(int ioa, List<Measurement> measurements) {
        UUID timeSeriesId = mappingIndex.getTimeSeriesId(ioa).orElse(null);
        if (timeSeriesId == null) {
            log.error("Missing TimeSeries ID for IOA {}. Dropping {} measurements.", ioa, measurements.size());
            return true;
        }

        try {
            coreClient.sendMeasurements(toTimeSeriesMeasurements(timeSeriesId, measurements));
            log.info("Sent {} measurements for IOA {}.", measurements.size(), ioa);
            return true;
        } catch (Exception ex) {
            log.error("Failed sending measurements for IOA {}. Retaining the batch for retry.", ioa, ex);
            return false;
        }
    }

    private List<Measurement> toTimeSeriesMeasurements(UUID timeSeriesId, List<Measurement> measurements) {
        return measurements.stream()
                .map(measurement -> new Measurement(timeSeriesId, measurement.getObservedAt(), measurement.getValue()))
                .toList();
    }
}
