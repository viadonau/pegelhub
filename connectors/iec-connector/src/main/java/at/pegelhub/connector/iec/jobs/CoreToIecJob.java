package at.pegelhub.connector.iec.jobs;

import at.pegelhub.connector.iec.datapoints.IecMappingIndex;
import at.pegelhub.connector.iec.iec.IecClient;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.model.Measurement;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

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
                                            latest -> iecClient.sendMeasurement(ioa, toIecMeasurement(ioa, latest)),
                                            () -> log.info("No measurement found for TimeSeries of IOA: {}.", ioa)),
                            () -> log.info("No TimeSeries ID configured for IOA: {}.", ioa))
            );
        } catch (Exception e) {
            log.info("Error sending measurements: {}", e.getMessage());
        }
    }

    private Measurement toIecMeasurement(int ioa, Measurement measurement) {
        return mappingIndex.getGaugeZeroElevationMAboveAdria(ioa)
                .map(gaugeZero -> new Measurement(
                        measurement.getTimeSeriesId(),
                        measurement.getObservedAt(),
                        BigDecimal.valueOf(measurement.getValue()).movePointLeft(2).add(gaugeZero).doubleValue()))
                .orElse(measurement);
    }
}
