package at.pegelhub.connector.tstp.task;

import at.pegelhub.connector.tstp.communication.TstpCommunicator;
import at.pegelhub.connector.tstp.communication.impl.TstpCommunicatorImpl;
import at.pegelhub.connector.tstp.ConnectorOptions;
import at.pegelhub.connector.tstp.service.impl.TstpBinaryServiceImpl;
import at.pegelhub.connector.tstp.service.impl.TstpXmlServiceImpl;
import at.pegelhub.connector.tstp.service.impl.TstpCatalogServiceImpl;
import at.pegelhub.lib.PegelHubCommunicator;
import at.pegelhub.lib.PegelHubCommunicatorFactory;
import at.pegelhub.lib.internal.ApplicationProperties;
import at.pegelhub.lib.internal.ApplicationPropertiesFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.util.TimerTask;

public class TstpTaskFactory {
    public static TimerTask getTstpTask(ConnectorOptions conOpt) throws MalformedURLException {
        PegelHubCommunicator phCommunicator = PegelHubCommunicatorFactory.create(URI.create(String.format("http://%s:%s/",
                conOpt.coreAddress(),
                conOpt.corePort())).toURL(), conOpt.propertiesFile());
        ApplicationProperties properties = ApplicationPropertiesFactory.create(conOpt.propertiesFile());
        TstpCommunicator tstpCommunicator = new TstpCommunicatorImpl(
                conOpt.tstpAddress(),
                conOpt.tstpPort(),
                HttpClient.newHttpClient(),
                new TstpXmlServiceImpl(new TstpBinaryServiceImpl()));

        int stationId = properties.getStationId();

        if (properties.isSupplier()) {
            return new TstpReader(phCommunicator,
                    tstpCommunicator,
                    conOpt.readDelay(),
                    conOpt.timeSeriesId(),
                    new TstpCatalogServiceImpl(tstpCommunicator, stationId));
        } else {
            return new TstpWriter(phCommunicator,
                    tstpCommunicator,
                    conOpt.readDelay().toSeconds()+"s",
                    conOpt.timeSeriesId(),
                    new TstpCatalogServiceImpl(tstpCommunicator, stationId));
        }
    }
}
