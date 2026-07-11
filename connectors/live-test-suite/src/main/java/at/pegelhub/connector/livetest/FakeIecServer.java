package at.pegelhub.connector.livetest;

import org.openmuc.j60870.ASdu;
import org.openmuc.j60870.ASduType;
import org.openmuc.j60870.CauseOfTransmission;
import org.openmuc.j60870.Connection;
import org.openmuc.j60870.ConnectionEventListener;
import org.openmuc.j60870.Server;
import org.openmuc.j60870.ServerEventListener;
import org.openmuc.j60870.ie.IeQuality;
import org.openmuc.j60870.ie.IeShortFloat;
import org.openmuc.j60870.ie.InformationElement;
import org.openmuc.j60870.ie.InformationObject;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Instant;

final class FakeIecServer implements AutoCloseable {
    private final HarnessState state;
    private final Server server;

    FakeIecServer(HarnessState state) throws IOException {
        this.state = state;
        this.server = Server.builder()
                .setBindAddr(InetAddress.getByName("0.0.0.0"))
                .setPort(SuiteConstants.IEC_PORT)
                .setIoaFieldLength(3)
                .setCommonAddressFieldLength(2)
                .setCotFieldLength(2)
                .build();
    }

    void start() throws IOException {
        server.start(new Listener());
    }

    @Override
    public void close() {
        server.stop();
    }

    private final class Listener implements ServerEventListener {
        @Override
        public void connectionIndication(Connection connection) {
            connection.setConnectionListener(new ConnectionListener(connection));
        }

        @Override
        public void serverStoppedListeningIndication(IOException e) {
        }

        @Override
        public void connectionAttemptFailed(IOException e) {
        }
    }

    private final class ConnectionListener implements ConnectionEventListener {
        private final Connection connection;
        private boolean sentReadScenario;

        private ConnectionListener(Connection connection) {
            this.connection = connection;
        }

        @Override
        public void newASdu(ASdu asdu) {
            try {
                if (asdu.getTypeIdentification() == ASduType.C_IC_NA_1) {
                    connection.sendConfirmation(asdu);
                    if (!sentReadScenario) {
                        sentReadScenario = true;
                        sendMeasurementToConnector(SuiteConstants.IEC_EXTERNAL_TO_CORE_IOA, 42.5);
                        sendMeasurementToConnector(SuiteConstants.IEC_UNMAPPED_IOA, 999.5);
                    }
                    connection.sendActivationTermination(asdu);
                    return;
                }

                if (asdu.getTypeIdentification() == ASduType.M_ME_NC_1) {
                    captureClientMeasurement(asdu);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void connectionClosed(IOException e) {
        }

        @Override
        public void dataTransferStateChanged(boolean stopped) {
        }

        private void sendMeasurementToConnector(int ioa, double value) throws IOException {
            ASdu asdu = new ASdu(
                    ASduType.M_ME_NC_1,
                    false,
                    CauseOfTransmission.SPONTANEOUS,
                    false,
                    false,
                    0,
                    SuiteConstants.IEC_COMMON_ADDRESS,
                    informationObject(ioa, value));
            state.iecCaptures.add(new IecCapture("server-to-connector", ioa, value, Instant.now()));
            connection.send(asdu);
        }

        private void captureClientMeasurement(ASdu asdu) {
            for (InformationObject object : asdu.getInformationObjects()) {
                InformationElement[][] elementSets = object.getInformationElements();
                if (elementSets.length == 0 || elementSets[0].length == 0) {
                    continue;
                }
                if (elementSets[0][0] instanceof IeShortFloat value) {
                    state.iecCaptures.add(new IecCapture(
                            "connector-to-server",
                            object.getInformationObjectAddress(),
                            value.getValue(),
                            Instant.now()));
                }
            }
        }
    }

    private static InformationObject informationObject(int ioa, double value) {
        return new InformationObject(
                ioa,
                new InformationElement[]{
                        new IeShortFloat((float) value),
                        new IeQuality(false, false, false, false, false),
                });
    }
}
