package at.pegelhub.connector.tstp.task;

import at.pegelhub.connector.tstp.communication.TstpCommunicator;
import at.pegelhub.connector.tstp.communication.impl.TstpCommunicatorImpl;
import at.pegelhub.connector.tstp.ConnectorOptions;
import at.pegelhub.connector.tstp.service.impl.TstpBinaryServiceImpl;
import at.pegelhub.connector.tstp.service.impl.TstpXmlServiceImpl;
import at.pegelhub.connector.tstp.service.impl.TstpCatalogServiceImpl;
import at.pegelhub.lib.config.MappingDirection;
import at.pegelhub.lib.PegelHubClient;

import java.net.http.HttpClient;

public class TstpTaskFactory {
    public static TstpRuntimeTask getTstpTask(ConnectorOptions conOpt, PegelHubClient coreClient) {
        TstpCommunicator tstpCommunicator = new TstpCommunicatorImpl(
                conOpt.tstpAddress(),
                conOpt.tstpPort(),
                HttpClient.newHttpClient(),
                new TstpXmlServiceImpl(new TstpBinaryServiceImpl()));
        TstpCatalogServiceImpl catalog = new TstpCatalogServiceImpl(tstpCommunicator, conOpt.stationId());

        if (conOpt.direction() == MappingDirection.EXTERNAL_TO_CORE) {
            var reader = new TstpReader(
                    coreClient,
                    tstpCommunicator,
                    conOpt.readDelay(),
                    conOpt.timeSeriesId(),
                    catalog);
            return new TstpRuntimeTask(reader, reader);
        }

        var writer = new TstpWriter(
                coreClient,
                tstpCommunicator,
                conOpt.readDelay().toSeconds() + "s",
                conOpt.timeSeriesId(),
                catalog);
        return new TstpRuntimeTask(writer, writer);
    }
}
