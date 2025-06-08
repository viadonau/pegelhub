package com.stm.pegelhub.connector.iec;

import com.stm.pegelhub.lib.PegelHubCommunicator;
import com.stm.pegelhub.lib.internal.ApplicationPropertiesImpl;
import com.stm.pegelhub.lib.model.Measurement;
import org.openmuc.j60870.*;
import org.openmuc.j60870.ie.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.*;

/**
 * Opens a connection to configured iec server and sends periodically an interrogation command.
 * An event-listener is registered to receive incoming messages from iec server.
 * ----------------------------------------------------------------------------------------------------
 * Queries periodically measurements for all defined stationNumbers and sends them to the iec server
 */
public class IecConnector implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(IecConnector.class);

    private final PegelHubCommunicator communicator;
    private Connection connection;
    private final Timer sleepInterval;
    private TimerTask task = null;
    private final ApplicationPropertiesImpl properties;
    private final ConnectorOptions conOpt;

    public IecConnector(PegelHubCommunicator communicator, Connection connection, ApplicationPropertiesImpl properties, ConnectorOptions conOpt) throws IOException {
        this.conOpt = conOpt;
        this.communicator = communicator;
        this.connection = connection;
        this.properties = properties;
        this.sleepInterval = new Timer();

        registerCallback(conOpt);
        if (conOpt.isReadingFromIec()) {
            initializeReadingTask();
        } else {
            initializeSendingTask();
        }

        sleepInterval.scheduleAtFixedRate(task, 0, conOpt.delay().toMillis());
    }

    /**
     * Initializes the task for querying (reading) data from the IEC server.
     * This method sets up a timer task that sends an interrogation command
     * to request data at regular intervals defined by the connector options.
     */
    private void initializeReadingTask() {
        task = new TimerTask() {
            @Override
            public void run() {
                try {
                    LOG.info("***********************************************");
                    LOG.info("Sending interrogation command to IEC server.");
                    LOG.info("***********************************************");

                    connection.synchronizeClocks(conOpt.common_address(), new IeTime56(System.currentTimeMillis()));

                    connection.interrogation(conOpt.common_address(), CauseOfTransmission.ACTIVATION, new IeQualifierOfInterrogation(20));
                } catch (IOException e) {
                    LOG.error("Error communicating with the IEC Server: {}", e.getMessage());
                    LOG.debug("Details:", e);
                    reconnectIfNecessary();
                }
            }
        };
    }

    /**
     * Reconnects to the IEC server if the connection is lost.
     * This method checks if the connection is still active and attempts to reconnect if necessary.
     */
    private void reconnectIfNecessary() {
            if (connection == null || connection.isClosed()) {
                LOG.info("Attempting to rebuild IEC connection...");
                try{
                    Connection newConnection = new ClientConnectionBuilder(conOpt.iec_host())
                            .setMessageFragmentTimeout(conOpt.message_fragment_timeout())
                            .setConnectionTimeout(conOpt.connection_timeout())
                            .setPort(conOpt.iec_port())
                            .build();

                    this.connection.close();
                    this.connection = newConnection;
                    registerCallback(conOpt);
                    LOG.info("Successfully reconnected to IEC server.");
                }
                catch (IOException e) {
                    LOG.error("Error reconnecting to IEC server: {}", e.getMessage());
                }
            }
    }

    /**
     * Initializes the task for sending data to the IEC server.
     * This method sets up a timer task that sends data transfer commands at regular intervals defined by the connector options.
     */
    private void initializeSendingTask() {
        task = new TimerTask() {
            @Override
            public void run() {
                LOG.info("**************************************");
                LOG.info("Sending measurements to IEC server.");
                LOG.info("**************************************");

                for (String stationNumber : conOpt.stationNumbers()) {
                    try {
                        List<Measurement> measurements = communicator.getMeasurementsOfStation(stationNumber, conOpt.delayString()).stream().toList();
                        LOG.info("Found {} measurements for station {}", measurements.size(), stationNumber);

                        Map<Integer, List<Measurement>> measurementsByIoa = new HashMap<>();

                        for (int i = 0; i < measurements.size(); i++) {
                            Measurement measurement = measurements.get(i);
                            Map<String, String> infos = measurement.getInfos();

                            int ioa = i + 1;
                            if (infos.containsKey("IOA") && infos.get("IOA") != null) {
                                try {
                                    ioa = Integer.parseInt(infos.get("IOA"));
                                } catch (NumberFormatException e) {
                                    LOG.warn("Invalid IOA in infos: {}, using {}", infos.get("IOA"), ioa);
                                }
                            }
                            measurementsByIoa.computeIfAbsent(ioa, k -> new ArrayList<>()).add(measurement);
                        }

                        for (Map.Entry<Integer, List<Measurement>> entry : measurementsByIoa.entrySet()) {
                            int ioa = entry.getKey();
                            List<Measurement> measurementsForIoa = entry.getValue();

                            processAndSendMeasurements(ioa, measurementsForIoa);
                        }

                    } catch (Exception e) {
                        LOG.error("Error sending measurements for station {}: {}", stationNumber, e.getMessage());
                        LOG.debug("Details:", e);
                        reconnectIfNecessary();
                    }
                }
            }
        };
    }

    /**
     * Processes a list of Measurement objects and then sends them to the configured IEC server.
     * @param ioa Information Object Address (IOA) associated with these measurements
     * @param measurements A list of Measurement objects
     * @throws IOException If an I/O error occurs during the communication with the IEC server
     * @throws InterruptedException If the current thread is interrupted
     */
    private void processAndSendMeasurements(int ioa, List<Measurement> measurements) throws IOException, InterruptedException {
        List<InformationElement[]> elements = new ArrayList<>();

        for (Measurement measurement : measurements) {
            Map<String, Double> fields = measurement.getFields();

            if (fields.isEmpty()) {
                LOG.warn("Measurement has no values, skipping: {}", measurement.getInfos());
                continue;
            }

            for (Map.Entry<String, Double> field : fields.entrySet()) {
                String fieldName = field.getKey();
                Double value = field.getValue();

                if (value == null) {
                    LOG.warn("Missing value for field '{}' in measurement {}", fieldName, measurement.getInfos());
                    continue;
                }

                InformationElement[] element = {
                        new IeShortFloat(value.floatValue()),
                        new IeQuality(false, false, false, false, false)
                };

                elements.add(element);
            }
        }

        if (elements.isEmpty()) {
            LOG.warn("No valid measurements to send for IOA {}", ioa);
            return;
        }

        InformationElement[][] ieArray = elements.toArray(new InformationElement[0][]);
        InformationObject informationObject = new InformationObject(ioa, ieArray);

        ASdu asdu = new ASdu(
                ASduType.M_ME_TF_1, true,
                CauseOfTransmission.SPONTANEOUS, false,
                false, 0, conOpt.common_address(), informationObject);

        LOG.info("Sending ASdu with IOA {} and {} elements", ioa, elements.size());
        connection.send(asdu);

        // Short break in between sending the new ASdu
        Thread.sleep(100);
    }

    /**
     * Registers a callback to handle incoming data transfer requests from the IEC server.
     * This method attempts to start data transfer and handles any exceptions that may occur.
     * @param conOpt Connector options containing configuration parameters
     */
    private void registerCallback(ConnectorOptions conOpt) {
        boolean connected = false;
        int retryCount = 1;

        while (!connected && retryCount <= conOpt.start_dt_retries()) {
            try {
                LOG.info(String.format("Send start DT. Try no. %d", retryCount));
                // handle iec events and receive data from iec
                connection.startDataTransfer(new IecClientEventListener(communicator, conOpt, properties, connection));
                connected = true;
            } catch (InterruptedIOException e2) {
                if (retryCount == conOpt.start_dt_retries()) {
                    LOG.error("Starting data transfer timed out. Closing connection. Because of no more retries.");
                    connection.close();
                    return;
                } else {
                    LOG.info("Got Timeout.class Next try.");
                    ++retryCount;
                }
            } catch (IOException e) {
                LOG.error(String.format("Connection closed for the following reason: %s", e.getMessage()));
                return;
            }
        }
        if (connected) {
            LOG.info("Successfully connected to IEC server");
        }
    }

    @Override
    public void close() {
        if (connection != null) {
            connection.close();
        }

        if (sleepInterval != null) {
            sleepInterval.cancel();
        }
        LOG.info("Closing IEC connection");
    }
}