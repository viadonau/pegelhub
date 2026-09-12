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
                mappingIndex.getTimeSeriesId(ioa).ifPresentOrElse(
                        timeSeriesId -> latestMeasurement(ioa, timeSeriesId).ifPresentOrElse(
                                latest -> iecClient.sendMeasurement(ioa, latest),
                                () -> log.info("No measurement found for TimeSeries of IOA: {}.", ioa)),
                        () -> log.info("No TimeSeries ID configured for IOA: {}.", ioa));
            } catch (Exception e) {
                log.warn("Error sending measurement for IOA {}", ioa, e);
            }
        }
    }

    private Optional<Measurement> latestMeasurement(int ioa, UUID timeSeriesId) {
        var representation = mappingIndex.getOutputRepresentation(ioa);
        return representation == MeasurementRepresentation.CANONICAL
                ? coreClient.getLatestMeasurementOfTimeSeries(timeSeriesId)
                : coreClient.getLatestMeasurementOfTimeSeries(timeSeriesId, representation);
    }
}
