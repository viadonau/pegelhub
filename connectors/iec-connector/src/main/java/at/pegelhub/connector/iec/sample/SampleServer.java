package at.pegelhub.connector.iec.sample;

import org.openmuc.j60870.*;
import org.openmuc.j60870.Server.Builder;
import org.openmuc.j60870.ie.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

/**
 * Iec test server. Only for testing!
 */
public class SampleServer {

    private static final Logger LOG = LoggerFactory.getLogger(ServerListener.class);

    private static final String ADRESS = "0.0.0.0";
    private static final int PORT = 2404;
    private static final int IAO_LENGTH = 3;
    private static final int COT_LENTH = 2;
    private static final int CA_LENGTH = 2;
    private int connectionIdCounter = 1;

    public class ServerListener implements ServerEventListener {

        public class ConnectionListener implements ConnectionEventListener {
            private final Connection connection;
            private final int connectionId;
            private boolean selected = false;

            public ConnectionListener(Connection connection, int connectionId) {
                this.connection = connection;
                this.connectionId = connectionId;
            }

            @Override
            public void newASdu(ASdu aSdu) {
                LOG.info("Got new ASdu:");
                println(aSdu.toString(), "\n");
                InformationObject informationObject = null;
                try {
                    switch (aSdu.getTypeIdentification()) {
                        // interrogation command
                        case C_IC_NA_1:
                            LOG.info("Got interrogation command (100). Will send scaled measured values.");
                            connection.sendConfirmation(aSdu);
                            // example GI response values
                            connection.send(new ASdu(ASduType.M_ME_NB_1, true, CauseOfTransmission.INTERROGATED_BY_STATION,
                                    false, false, 0, aSdu.getCommonAddress(),
                                    new InformationObject(1,
                                            new InformationElement[][] {
                                                    {
                                                            new IeScaledValue(new Random().nextInt(0, 2000)),
                                                            new IeQuality(false, false, false, false, false),
                                                    },
                                                    {
                                                            new IeScaledValue(new Random().nextInt(0, 2000)),
                                                            new IeQuality(false, false, false, false, false),
                                                    },
                                                    {
                                                            new IeScaledValue(new Random().nextInt(0, 2000)),
                                                            new IeQuality(false, false, false, false, false),
                                                    },
                                                    {
                                                            new IeScaledValue(new Random().nextInt(0, 2000)),
                                                            new IeQuality(false, false, false, false, false),
                                                    }
                                            }
                                    )));

                            connection.sendActivationTermination(aSdu);
                            break;
                        case C_SC_NA_1:
                            informationObject = aSdu.getInformationObjects()[0];
                            IeSingleCommand singleCommand = (IeSingleCommand) informationObject
                                    .getInformationElements()[0][0];

                            if (informationObject.getInformationObjectAddress() != 5000) {
                                break;
                            }
                            if (singleCommand.isSelect()) {
                                LOG.info("Got single command (45) with select true. Select command.");
                                selected = true;
                                connection.sendConfirmation(aSdu);
                            }
                            else if (!singleCommand.isSelect() && selected) {
                                LOG.info("Got single command (45) with select false. Execute selected command.");
                                selected = false;
                                connection.sendConfirmation(aSdu);
                            }
                            else {
                                LOG.info("Got single command (45) with select false. But no command is selected, no execution.");
                            }
                            break;
                        case C_CS_NA_1:
                            IeTime56 ieTime56 = new IeTime56(System.currentTimeMillis());
                            LOG.info("Got Clock synchronization command (103). Send current time: \n", ieTime56.toString());
                            connection.synchronizeClocks(aSdu.getCommonAddress(), ieTime56);
                            break;
                        case C_SE_NB_1:
                            LOG.info("Got Set point command, scaled value (49)");
                            break;
                        case M_ME_TF_1:
                            LOG.info("Got floating point value. Command 36");
                            break;
                        default:
                            LOG.info("Got unknown request: ", aSdu.toString(),
                                    ". Send negative confirm with CoT UNKNOWN_TYPE_ID(44)\n");
                            connection.sendConfirmation(aSdu, aSdu.getCommonAddress(), true,
                                    CauseOfTransmission.UNKNOWN_TYPE_ID);
                    }

                } catch (EOFException e) {
                    LOG.error("Will quit listening for commands on connection (" + connectionId + ") because socket was closed. " + e.getMessage());
                } catch (IOException e) {
                    LOG.error("Will quit listening for commands on connection ({}) because of error: {}", connectionId, e.getMessage(), e);
                }

            }

            @Override
            public void connectionClosed(IOException e) {
                LOG.info("Connection (" + connectionId, ") was closed. ", e.getMessage());
            }

            @Override
            public void dataTransferStateChanged(boolean stopped) {
                LOG.info(String.format("Data tranfer: %s", stopped ? "stopped" : "started"));
            }
        }

        @Override
        public void connectionIndication(Connection connection) {
            int myConnectionId = connectionIdCounter++;
            LOG.info("A client (Originator Address " + connection.getOriginatorAddress() + ") has connected using TCP/IP. Will listen for a StartDT request. Connection ID: " + myConnectionId);
            LOG.info("Started data transfer on connection (" + myConnectionId + ") Will listen for incoming commands.");
            connection.setConnectionListener(new ConnectionListener(connection, myConnectionId));
        }

        @Override
        public void serverStoppedListeningIndication(IOException e) {
            LOG.info("Server has stopped listening for new connections : \"", e.getMessage(), "\". Will quit.");
        }

        @Override
        public void connectionAttemptFailed(IOException e) {
            LOG.info("Connection attempt failed: ", e.getMessage());
        }
    }

    public static void main(String[] args) throws UnknownHostException {
        new SampleServer().start();
    }


    public void start() throws UnknownHostException {
        LOG.info("### Starting Server ###\n" +
                "\nBind Address: " + ADRESS +
                "\nPort:         " + PORT +
                "\nIAO length:   "+ IAO_LENGTH +
                "\nCA length:    "+ CA_LENGTH +
                "\nCOT length:   "+ COT_LENTH +
                "\n");

        Builder builder = Server.builder();
        InetAddress bindAddress = InetAddress.getByName(ADRESS);
        builder.setBindAddr(bindAddress)
                .setPort(PORT)
                .setIoaFieldLength(IAO_LENGTH)
                .setCommonAddressFieldLength(CA_LENGTH)
                .setCotFieldLength(COT_LENTH);// .setMaxNumOfOutstandingIPdus(10);
        Server server = builder.build();

        try {
            server.start(new ServerListener());
        } catch (IOException e) {
            LOG.info("Unable to start listening: \"" + e.getMessage() + "\". Will quit.");
        }
    }

    private void println(String string, String... strings) {
        StringBuilder sb = new StringBuilder();
        sb.append(string);
        for (String s : strings) {
            sb.append(s);
        }
    }

}
