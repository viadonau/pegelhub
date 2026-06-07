package at.pegelhub.connector.tstp.task;

import at.pegelhub.connector.tstp.communication.TstpCommunicator;
import at.pegelhub.connector.tstp.service.TstpCatalogService;
import at.pegelhub.lib.PegelHubCommunicator;
import at.pegelhub.lib.model.Measurement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.UUID;

public class TstpReader extends TimerTask {
    private static final Logger LOG = LoggerFactory.getLogger(TstpReader.class);
    private final TstpCommunicator tstpCommunicator;
    private final PegelHubCommunicator phCommunicator;
    private final Duration durationToLookBack;
    private final UUID timeSeriesId;
    private final TstpCatalogService tstpCatalogService;

    public TstpReader(PegelHubCommunicator phCommunicator, TstpCommunicator tstpCommunicator, Duration durationToLookBack, UUID timeSeriesId, TstpCatalogService tstpCatalogService) {
        this.phCommunicator = phCommunicator;
        this.durationToLookBack = durationToLookBack;
        this.timeSeriesId = timeSeriesId;
        this.tstpCommunicator = tstpCommunicator;
        this.tstpCatalogService = tstpCatalogService;
    }

    /**
     * The connection to the TSTP Server. Reads the file and tries to parse it. If successful, the parsed Measurements get
     * transferred to Pegelhub Core
     */
    @Override
    public void run() {
        try {
            String zrid = tstpCatalogService.getZrid();
            LOG.info("ZRID gotten from catalog: " + zrid);

            List<Measurement> measurements = tstpCommunicator.getMeasurements(zrid, getLookBackTimestamp(), Instant.now());
            LOG.info("Read in measurements from tstp server");
            if (!measurements.isEmpty()) {
                phCommunicator.sendMeasurements(measurements.stream()
                        .map(this::withTimeSeriesId)
                        .toList());
                LOG.info("Sent measurements to core");
            } else {
                LOG.info("Measurement List is empty - nothing was sent to the core");
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

    private Instant getLookBackTimestamp() {
        return Instant.now().minus(durationToLookBack);
    }

    private Measurement withTimeSeriesId(Measurement measurement) {
        return new Measurement(timeSeriesId, measurement.getObservedAt(), measurement.getValue());
    }
}
