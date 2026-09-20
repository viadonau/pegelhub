package at.pegelhub.watchdog.lab;

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
import java.util.concurrent.CopyOnWriteArrayList;

/** Pausing emissions intentionally leaves STARTDT and the TCP connection intact. */
final class IecFixture implements AutoCloseable {
    private final Server server = Server.builder().setPort(2404).build();
    private final CopyOnWriteArrayList<Connection> active = new CopyOnWriteArrayList<>();
    volatile boolean paused;

    IecFixture() throws IOException {
        server.start(new ServerEventListener() {
            @Override
            public void connectionIndication(Connection connection) {
                connection.setConnectionListener(new ConnectionEventListener() {
                    @Override
                    public void newASdu(ASdu asdu) {
                        if (asdu.getTypeIdentification() == ASduType.C_IC_NA_1) {
                            active.addIfAbsent(connection);
                            try {
                                connection.sendConfirmation(asdu);
                                connection.sendActivationTermination(asdu);
                            } catch (IOException failure) {
                                active.remove(connection);
                            }
                        }
                    }

                    @Override
                    public void connectionClosed(IOException failure) {
                        active.remove(connection);
                    }

                    @Override
                    public void dataTransferStateChanged(boolean stopped) {
                        if (stopped) {
                            active.remove(connection);
                        } else {
                            active.addIfAbsent(connection);
                        }
                    }
                });
            }

            @Override
            public void serverStoppedListeningIndication(IOException failure) {
            }

            @Override
            public void connectionAttemptFailed(IOException failure) {
            }
        });
    }

    void emit() {
        if (paused) {
            return;
        }
        for (Connection connection : active) {
            try {
                // A constant level still produces fresh IEC receipts; the watchdog must not require value changes.
                var object = new InformationObject(66051, new InformationElement[][]{
                        {new IeShortFloat(281.0f), new IeQuality(false, false, false, false, false)}
                });
                connection.send(new ASdu(ASduType.M_ME_NC_1, false, CauseOfTransmission.SPONTANEOUS,
                        false, false, 0, 1, object));
            } catch (IOException failure) {
                active.remove(connection);
            }
        }
    }

    int connections() {
        return active.size();
    }

    @Override
    public void close() {
        server.stop();
    }
}
