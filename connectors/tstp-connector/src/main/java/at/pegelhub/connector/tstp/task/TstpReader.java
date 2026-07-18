package at.pegelhub.connector.tstp.task;

import at.pegelhub.connector.tstp.communication.TstpCommunicator;
import at.pegelhub.connector.tstp.service.TstpCatalogService;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.model.Measurement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class TstpReader implements Runnable, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TstpReader.class);
    private final TstpCommunicator tstpCommunicator;
    private final PegelHubClient coreClient;
    private final Duration durationToLookBack;
    private final UUID timeSeriesId;
    private final TstpCatalogService tstpCatalogService;

    public TstpReader(PegelHubClient coreClient, TstpCommunicator tstpCommunicator, Duration durationToLookBack, UUID timeSeriesId, TstpCatalogService tstpCatalogService) {
        this.coreClient = coreClient;
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
                coreClient.sendMeasurements(measurements.stream()
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
    public void close() throws Exception {
        coreClient.close();
    }

    private Instant getLookBackTimestamp() {
        return Instant.now().minus(durationToLookBack);
    }

    private Measurement withTimeSeriesId(Measurement measurement) {
        return new Measurement(timeSeriesId, measurement.getObservedAt(), measurement.getValue());
    }
}
