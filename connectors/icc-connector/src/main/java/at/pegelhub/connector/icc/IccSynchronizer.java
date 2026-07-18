package at.pegelhub.connector.icc;

import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.config.MappingDirection;
import at.pegelhub.lib.exception.NotFoundException;
import at.pegelhub.lib.model.Measurement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.time.Duration;

/**
 * Task of syncing selected TimeSeries measurements between the local Core and an external Core.
 */
public class IccSynchronizer implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(IccSynchronizer.class);
    private final PegelHubClient coreClient;
    private final PegelHubClient externalClient;
    private final List<IccMapping> mappings;
    private final Duration lookback;

    public IccSynchronizer(
            PegelHubClient coreClient,
            PegelHubClient externalClient,
            List<IccMapping> mappings,
            Duration lookback) {
        this.coreClient = coreClient;
        this.externalClient = externalClient;
        this.mappings = mappings;
        this.lookback = lookback;
    }

    /**
     * Fetches recent TimeSeries data within {@code lookbackWindow} from one Core and sends it to the other.
     */
    @Override
    public void run() {
        for (IccMapping mapping : mappings) {
            if (mapping.direction() == MappingDirection.CORE_TO_EXTERNAL) {
                sync(coreClient, externalClient, mapping.timeSeriesId(), mapping.externalTimeSeriesId());
            } else {
                sync(externalClient, coreClient, mapping.externalTimeSeriesId(), mapping.timeSeriesId());
            }
        }
    }

    private void sync(PegelHubClient source, PegelHubClient target, UUID sourceTimeSeriesId, UUID targetTimeSeriesId) {
        try {
            List<Measurement> measurements = source.getMeasurementsOfTimeSeries(sourceTimeSeriesId, lookback).stream()
                    .map(measurement -> new Measurement(
                            targetTimeSeriesId,
                            measurement.getObservedAt(),
                            measurement.getValue()))
                    .toList();
            target.sendMeasurements(measurements);
        } catch (NotFoundException nfe) {
            LOG.error("No data found for TimeSeries " + sourceTimeSeriesId);
        } catch (Exception ex) {
            LOG.error("Error when syncing data for TimeSeries " + sourceTimeSeriesId);
        }
    }
}
