package at.pegelhub.connector.icc;

import at.pegelhub.lib.PegelHubCommunicator;
import at.pegelhub.lib.exception.NotFoundException;
import at.pegelhub.lib.model.Measurement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Task of syncing data from one source hub to the sink hub.
 * Only fetches the newly added relevant data points of the configured TimeSeries from the source.
 */
public class IccTask extends TimerTask {

    private static final Logger LOG = LoggerFactory.getLogger(IccTask.class);
    private final PegelHubCommunicator source;
    private final PegelHubCommunicator sink;
    private final List<UUID> timeSeriesIds;
    private final String refreshInterval;

    public IccTask(PegelHubCommunicator source, PegelHubCommunicator sink, List<UUID> timeSeriesIds, String refreshInterval) {
        this.source = source;
        this.sink = sink;
        this.timeSeriesIds = timeSeriesIds;
        this.refreshInterval = refreshInterval;
    }

    /**
     * Fetches recent TimeSeries data within {@code refreshInterval} from the source and sends it to the sink.
     */
    @Override
    public void run() {
        for (UUID timeSeriesId : timeSeriesIds) {
            try {
                List<Measurement> measurements = source.getMeasurementsOfTimeSeries(timeSeriesId, refreshInterval).stream().toList();
                sink.sendMeasurements(measurements);
            } catch (NotFoundException nfe) {
                LOG.error("No data found for TimeSeries " + timeSeriesId);
            } catch (Exception ex) {
                LOG.error("Error when syncing data for TimeSeries " + timeSeriesId);
            }
        }
    }
}
