package at.pegelhub.connector.icc;

import at.pegelhub.lib.PegelHubCommunicator;
import at.pegelhub.lib.PegelHubCommunicatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Implementation of an Icc connector that periodically fetches data of the source defined in the config file.
 * Data is fetched from the source hub and sent to the sink hub based on the entries in the config file.
 * Data is synced in a time interval defined in the config file.
 */
public class IccConnector implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(IccConnector.class);
    private final PegelHubCommunicator source;
    private final PegelHubCommunicator sink;
    private final Timer sleepInterval;
    private final TimerTask task;

    public IccConnector(URL sourceUrl, String sourceProperties, URL sinkUrl, String sinkProperties, List<UUID> timeSeriesIds, Duration delay, String refreshInterval) {
        source = PegelHubCommunicatorFactory.create(sourceUrl, sourceProperties);
        sink = PegelHubCommunicatorFactory.create(sinkUrl, sinkProperties);

        sleepInterval = new Timer();
        task = new IccTask(source, sink, timeSeriesIds, refreshInterval);
        sleepInterval.scheduleAtFixedRate(task, 0, delay.toMillis());
    }

    @Override
    public void close() throws Exception {
        LOG.info("ICC-Connector closed");
        sleepInterval.cancel();
        task.cancel();
        source.close();
        sink.close();
    }
}
