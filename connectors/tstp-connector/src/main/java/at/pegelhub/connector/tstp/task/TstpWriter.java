package at.pegelhub.connector.tstp.task;

import at.pegelhub.connector.tstp.communication.TstpCommunicator;
import at.pegelhub.connector.tstp.service.TstpCatalogService;
import at.pegelhub.lib.PegelHubCommunicator;
import at.pegelhub.lib.model.Measurement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.UUID;

public class TstpWriter extends TimerTask {
    private static final Logger LOG = LoggerFactory.getLogger(TstpWriter.class);
    private final TstpCommunicator tstpCommunicator;
    private final PegelHubCommunicator phCommunicator;
    private final String durationToLookBack;
    private final UUID timeSeriesId;
    private final TstpCatalogService tstpCatalogService;

    public TstpWriter(PegelHubCommunicator phCommunicator, TstpCommunicator tstpCommunicator, String durationToLookBack, UUID timeSeriesId, TstpCatalogService tstpCatalogService) {
        this.phCommunicator = phCommunicator;
        this.durationToLookBack = durationToLookBack;
        this.tstpCommunicator = tstpCommunicator;
        this.timeSeriesId = timeSeriesId;
        this.tstpCatalogService = tstpCatalogService;
    }

    /**
     * The connection to the TSTP Server. Reads the file and tries to parse it. If successful, the parsed Measurements get
     * transferred to Pegelhub Core
     */
    @Override
    public void run() {
        try {
            List<Measurement> measurements = phCommunicator.getMeasurementsOfTimeSeries(timeSeriesId, durationToLookBack).stream().toList();
            String zrid = tstpCatalogService.getZrid();

            if (!measurements.isEmpty()) {
                tstpCommunicator.sendMeasurements(zrid, measurements);
            } else {
                LOG.info("Measurement list is empty - nothing was sent");
            }
        } catch (Exception e) {
            LOG.error("Unhandled Exception was thrown!", e);
        }
    }

    @Override
    public boolean cancel() {
        try {
            phCommunicator.close();
            return super.cancel();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
