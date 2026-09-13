package at.pegelhub.connector.iec.jobs;

import at.pegelhub.connector.iec.datapoints.IecMappingIndex;
import at.pegelhub.connector.iec.iec.IecClient;
import at.pegelhub.lib.PegelHubClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Sends the latest Core value for each outgoing mapping. Core handles any unit or reference-level conversion.
 * If reading fails, including a failed representation check, nothing is sent for that IOA.
 * There is no fallback to canonical values. The remaining mappings still run, and failed mappings
 * are tried again on the next scheduled run.
 */
@Slf4j
@AllArgsConstructor
public class CoreToIecJob implements Runnable {
    private final IecClient iecClient;
    private final IecMappingIndex mappingIndex;
    private final PegelHubClient coreClient;

    @Override
    public void run() {
        for (var mapping : mappingIndex.coreToProtocolMappings()) {
            try {
                var latest = coreClient.getLatestMeasurementOfTimeSeries(
                        mapping.timeSeriesId(), mapping.outputRepresentation());
                if (latest.isEmpty()) {
                    log.info("No measurement found for TimeSeries of IOA: {}.", mapping.iecIoa());
                    continue;
                }

                iecClient.sendMeasurement(mapping.iecIoa(), latest.get());
            } catch (Exception e) {
                log.warn("Error sending measurement for IOA {}", mapping.iecIoa(), e);
            }
        }
    }
}
