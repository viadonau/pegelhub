package at.pegelhub.connector.iec.jobs;

import at.pegelhub.connector.iec.datapoints.IecMappingIndex;
import at.pegelhub.connector.iec.iec.IecClient;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.model.Measurement;
import at.pegelhub.lib.model.MeasurementRepresentation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@AllArgsConstructor
public class CoreToIecJob implements Runnable {
    private final IecClient iecClient;
    private final IecMappingIndex mappingIndex;
    private final PegelHubClient coreClient;

    @Override
    public void run() {
        for (int ioa : mappingIndex.coreToProtocolIoas()) {
            try {
                sendLatestMeasurement(ioa);
            } catch (Exception e) {
                log.warn("Error sending measurement for IOA {}", ioa, e);
            }
        }
    }

    private void sendLatestMeasurement(int ioa) {
        Optional<UUID> timeSeriesId = mappingIndex.getTimeSeriesId(ioa);
        if (timeSeriesId.isEmpty()) {
            log.info("No TimeSeries ID configured for IOA: {}.", ioa);
            return;
        }

        Optional<Measurement> latest = latestMeasurement(ioa, timeSeriesId.get());
        if (latest.isEmpty()) {
            log.info("No measurement found for TimeSeries of IOA: {}.", ioa);
            return;
        }

        iecClient.sendMeasurement(ioa, latest.get());
    }

    private Optional<Measurement> latestMeasurement(int ioa, UUID timeSeriesId) {
        var representation = mappingIndex.getOutputRepresentation(ioa);
        if (representation == MeasurementRepresentation.CANONICAL) {
            return coreClient.getLatestMeasurementOfTimeSeries(timeSeriesId);
        }
        return coreClient.getLatestMeasurementOfTimeSeries(timeSeriesId, representation);
    }
}
