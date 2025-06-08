package com.stm.pegelhub.connector.iec;

import com.stm.pegelhub.lib.PegelHubCommunicator;
import com.stm.pegelhub.lib.PegelHubCommunicatorFactory;
import com.stm.pegelhub.lib.internal.ApplicationPropertiesImpl;
import org.openmuc.j60870.ClientConnectionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        ConnectorOptions conOpt = null;
        PegelHubCommunicator communicator = null;
        org.openmuc.j60870.Connection iecConnection = null;
        ApplicationPropertiesImpl applicationProperties = null;

        LOG.info("Starting IEC Connector...");

        try{
            conOpt = ConfigLoader.fromArgs(args);
            applicationProperties = new ApplicationPropertiesImpl(conOpt.propertyFileName());
            LOG.info("ApplicationProperties loaded successfully from {}.", conOpt.propertyFileName());

            communicator = PegelHubCommunicatorFactory.create(new URL(String.format("http://%s:%s/",
                    conOpt.coreAddress().getHostAddress(), conOpt.corePort())), conOpt.propertyFileName());

            iecConnection = new ClientConnectionBuilder(conOpt.iec_host())
                    .setMessageFragmentTimeout(conOpt.message_fragment_timeout())
                    .setConnectionTimeout(conOpt.connection_timeout())
                    .setPort(conOpt.iec_port())
                    .build();
            LOG.info("IEC Connection established to {}:{}.", conOpt.iec_host(), conOpt.iec_port());

            IecConnector connector = new IecConnector(communicator, iecConnection, applicationProperties, conOpt);

            Runtime.getRuntime().addShutdownHook(new Thread(connector::close));

            try {
                LOG.info("IecConnector successfully initialized and running.");
                Thread.currentThread().join();
            }
            catch (Exception e) {
                LOG.error("An unexpected error occurred: {}", e.getMessage(), e);
                LOG.debug("Details:", e);
                System.exit(1);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
