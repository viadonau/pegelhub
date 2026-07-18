package at.pegelhub.connector.tstp.task;

import at.pegelhub.connector.tstp.TstpMapping;
import at.pegelhub.connector.tstp.communication.TstpCommunicator;
import at.pegelhub.connector.tstp.communication.impl.TstpCommunicatorImpl;
import at.pegelhub.connector.tstp.config.TstpConnectorConfig;
import at.pegelhub.connector.tstp.service.impl.TstpBinaryServiceImpl;
import at.pegelhub.connector.tstp.service.impl.TstpXmlServiceImpl;
import at.pegelhub.connector.tstp.service.impl.TstpCatalogServiceImpl;
import at.pegelhub.lib.config.MappingDirection;
import at.pegelhub.lib.PegelHubClient;

import java.net.http.HttpClient;

public class TstpTaskFactory {
    public static TstpRuntimeTask getTstpTask(TstpConnectorConfig config, PegelHubClient coreClient) {
        TstpCommunicator tstpCommunicator = new TstpCommunicatorImpl(
                config.server().host(),
                config.server().port(),
                HttpClient.newHttpClient(),
                new TstpXmlServiceImpl(new TstpBinaryServiceImpl()));
        TstpMapping mapping = config.mapping();
        TstpCatalogServiceImpl catalog = new TstpCatalogServiceImpl(tstpCommunicator, mapping.stationId());

        if (mapping.direction() == MappingDirection.EXTERNAL_TO_CORE) {
            var reader = new TstpReader(
                    coreClient,
                    tstpCommunicator,
                    config.pollInterval(),
                    mapping.timeSeriesId(),
                    catalog);
            return new TstpRuntimeTask(reader, reader);
        }

        var writer = new TstpWriter(
                coreClient,
                tstpCommunicator,
                config.pollInterval(),
                mapping.timeSeriesId(),
                catalog);
        return new TstpRuntimeTask(writer, writer);
    }
}
