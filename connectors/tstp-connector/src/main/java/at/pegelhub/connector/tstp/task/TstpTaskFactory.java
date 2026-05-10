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
import at.pegelhub.lib.internal.dto.SupplierSendDto;
import at.pegelhub.lib.internal.dto.TakerSendDto;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.http.HttpClient;
import java.util.TimerTask;

public class TstpTaskFactory {
    public static TimerTask getTstpTask(ConnectorOptions conOpt) throws MalformedURLException {
        PegelHubCommunicator phCommunicator = PegelHubCommunicatorFactory.create(new URL(
                String.format("http://%s:%s/",
                        conOpt.coreAddress(),
                        conOpt.corePort())), conOpt.propertiesFile());
        ApplicationProperties properties = ApplicationPropertiesFactory.create(conOpt.propertiesFile());
        TstpCommunicator tstpCommunicator = new TstpCommunicatorImpl(
                conOpt.tstpAddress(),
                conOpt.tstpPort(),
                HttpClient.newHttpClient(),
                new TstpXmlServiceImpl(new TstpBinaryServiceImpl()));

        if (properties.isSupplier()) {
            SupplierSendDto supplier = properties.getSupplier();
            int stationId = supplier.stationId();

            return new TstpReader(phCommunicator,
                    tstpCommunicator,
                    conOpt.readDelay(),
                    new TstpCatalogServiceImpl(tstpCommunicator, stationId));
        } else {
            TakerSendDto taker = properties.getTaker();
            int stationId = taker.stationId();

            return new TstpWriter(phCommunicator,
                    tstpCommunicator,
                    conOpt.readDelay().toSeconds()+"s",
                    taker.stationNumber(),
                    new TstpCatalogServiceImpl(tstpCommunicator, stationId));
        }
    }
}
