package at.pegelhub.connector.icc;

import at.pegelhub.lib.PegelHubCommunicator;
import at.pegelhub.lib.exception.NotFoundException;
import at.pegelhub.lib.model.Measurement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.TimerTask;

/**
 * Task of syncing data from one source hub to the sink hub.
 * Only fetches the newly added relevant data points of the provided station numbers (of the source)s.
 */
public class IccTask extends TimerTask {

    private static final Logger LOG = LoggerFactory.getLogger(IccTask.class);
    private final PegelHubCommunicator source;
    private final PegelHubCommunicator sink;
    private final List<String> stationNumbers;
    private final String refreshInterval;

    public IccTask(PegelHubCommunicator source, PegelHubCommunicator sink, List<String> stationNumbers, String refreshInterval) {
        this.source = source;
        this.sink = sink;
        this.stationNumbers = stationNumbers;
        this.refreshInterval = refreshInterval;
    }

    /**
     * Fetches all recent data of {@code stationNumbers} within the interval {@code refreshInterval} of PH {@code source} and sends it to PH {@code sink}.
     */
    @Override
    public void run() {
        for (String stationNumber : stationNumbers) {
            try {
                List<Measurement> measurements = source.getMeasurementsOfStation(stationNumber, refreshInterval).stream().toList();
                sink.sendMeasurements(measurements);
            } catch (NotFoundException nfe) {
                LOG.error("No data found for station number " + stationNumber);
            } catch (Exception ex) {
                LOG.error("Error when syncing data for station " + stationNumber);
            }
        }


    }
}
