package at.pegelhub.connector.iec.iec.impl;

import at.pegelhub.connector.iec.iec.IecClient;
import at.pegelhub.lib.model.Measurement;
import lombok.extern.slf4j.Slf4j;
import org.openmuc.j60870.*;
import org.openmuc.j60870.ie.*;

import java.io.IOException;
import java.net.Socket;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

@Slf4j
public class IecClientImpl implements IecClient {
    private final String host;
    private final int port;
    private final int commonAddress;
    private final Set<Integer> registeredProtocolToCoreIoas;
    private final BlockingQueue<ReceivedMeasurement> measurementQueue = new LinkedBlockingQueue<>();
    private final Object lifecycle = new Object();
    private final IecConnectionOpener opener;
    private boolean stopped;
    private Attempt active;
    private Attempt inFlight;

    public IecClientImpl(String host, int port, int commonAddress, Set<Integer> registeredProtocolToCoreIoas) {
        this(host, port, commonAddress, registeredProtocolToCoreIoas, IecConnectionOpener::openTcp);
    }

    IecClientImpl(String host, int port, int commonAddress, Set<Integer> registeredProtocolToCoreIoas,
                  IecConnectionOpener opener) {
        this.host = host;
        this.port = port;
        this.commonAddress = commonAddress;
        this.registeredProtocolToCoreIoas = registeredProtocolToCoreIoas;
        this.opener = opener;
    }

    @Override
    public void connect() {
        Attempt previous;
        synchronized (lifecycle) {
            if (stopped || inFlight != null || Thread.currentThread().isInterrupted()) {
                return;
            }
            previous = active;
        }
        boolean healthy = previous != null && !previous.connection.isClosed() && !previous.connection.isStopped();
        Attempt attempt;
        synchronized (lifecycle) {
            if (stopped || inFlight != null || active != previous || (healthy && !previous.invalid)) {
                return;
            }
            active = null;
            attempt = new Attempt();
            inFlight = attempt;
        }
        try {
            close(previous);
            log.info("Attempting IEC connection to {}:{}", host, port);
            attempt.connection = opener.open(host, port, attempt.socket);
            if (!isCurrent(attempt) || Thread.currentThread().isInterrupted()) {
                return;
            }
            attempt.connection.startDataTransfer(createIecListener(attempt));
            // j60870 can return from STARTDT with the interrupt flag set.
            if (!isCurrent(attempt) || Thread.currentThread().isInterrupted()) {
                return;
            }
            attempt.connection.interrogation(commonAddress, CauseOfTransmission.ACTIVATION, new IeQualifierOfInterrogation(20));
            if (attempt.connection.isClosed() || attempt.connection.isStopped()) {
                throw new IOException("IEC connection closed or stopped during startup");
            }
            synchronized (lifecycle) {
                if (!isCurrent(attempt) || Thread.currentThread().isInterrupted()) {
                    return;
                }
                active = attempt;
            }
            log.info("Connected to IEC server at {}:{}", host, port);
        } catch (IOException e) {
            log.warn("IEC connection failed; a later scheduled attempt will retry: {}", e.toString());
        } finally {
            boolean published;
            synchronized (lifecycle) {
                published = active == attempt;
                if (inFlight == attempt) {
                    inFlight = null;
                }
            }
            if (!published) {
                close(attempt);
            }
        }
    }

    @Override
    public void disconnect() {
        Attempt previous;
        Attempt candidate;
        synchronized (lifecycle) {
            stopped = true;
            previous = active;
            candidate = inFlight;
            active = null;
            inFlight = null;
        }
        close(previous);
        close(candidate);
    }

    @Override
    public void sendMeasurement(int ioa, Measurement measurement) {
        Attempt current;
        synchronized (lifecycle) {
            current = stopped ? null : active;
            if (current != null && current.invalid) {
                current = null;
            }
        }
        if (current == null || current.connection.isClosed() || current.connection.isStopped()) {
            throw new IllegalStateException("IEC connection is unavailable; retry on a later poll");
        }
        float value = measurement.getValue().floatValue();

        InformationElement[] elements = new InformationElement[]{
                new IeShortFloat(value),
                new IeQuality(false, false, false, false, false),
        };

        InformationObject informationObject = new InformationObject(ioa, elements);

        ASdu asdu = new ASdu(
                ASduType.M_ME_NC_1,
                false,
                CauseOfTransmission.SPONTANEOUS,
                false,
                false,
                0,
                this.commonAddress,
                informationObject
        );

        log.info("Sending ASDU with IOA {} and value {}", ioa, value);

        try {
            current.connection.send(asdu);
        } catch (IOException e) {
            synchronized (lifecycle) {
                current.invalid = true;
            }
            close(current);
            throw new IllegalStateException("Failed sending IEC measurement", e);
        }
    }

    @Override
    public Map<Integer, List<Measurement>> drainGroupedMeasurements() {
        List<ReceivedMeasurement> drained = new ArrayList<>();
        measurementQueue.drainTo(drained);

        if (drained.isEmpty()) {
            return Collections.emptyMap();
        }

        return drained.stream().collect(Collectors.groupingBy(
                ReceivedMeasurement::ioa,
                Collectors.mapping(ReceivedMeasurement::measurement, Collectors.toList())
        ));
    }

    private boolean isCurrent(Attempt attempt) {
        synchronized (lifecycle) {
            return !stopped && !attempt.invalid && (active == attempt || inFlight == attempt);
        }
    }

    private void close(Attempt attempt) {
        if (attempt == null) return;
        // Close the owned transport first, including a build still in progress.
        try {
            attempt.socket.close();
        } catch (IOException e) {
            log.debug("Error closing IEC socket", e);
        }
        Connection connection = attempt.connection;
        if (connection != null) connection.close();
    }

    private static final class Attempt {
        final Socket socket = new Socket();
        volatile Connection connection;
        boolean invalid;
    }

    private ConnectionEventListener createIecListener(Attempt attempt) {
        return new ConnectionEventListener() {
            @Override
            public void newASdu(ASdu aSdu) {
                synchronized (lifecycle) {
                    if (isCurrent(attempt)) enqueueMeasurements(aSdu);
                }
            }

            @Override
            public void connectionClosed(IOException e) {
                invalidate("closed");
            }

            @Override
            public void dataTransferStateChanged(boolean stopped) {
                if (stopped) invalidate("STOPDT");
            }

            private void invalidate(String reason) {
                synchronized (lifecycle) {
                    if (!isCurrent(attempt)) return;
                    attempt.invalid = true;
                }
                log.info("IEC connection {}; scheduled recovery will retry", reason);
            }
        };
    }

    private void enqueueMeasurements(ASdu aSdu) {
        ASduType t = aSdu.getTypeIdentification();
        if (t != ASduType.M_ME_NC_1 && t != ASduType.M_ME_TF_1) {
            return;
        }

        Arrays.stream(aSdu.getInformationObjects()).forEach(io -> {
            int ioa = io.getInformationObjectAddress();

            if (!registeredProtocolToCoreIoas.contains(ioa)) {
                log.debug("Ignoring IOA {} (no external-to-core mapping registered).", ioa);
                return;
            }

            InformationElement[][] sets = io.getInformationElements();
            if (sets.length == 0) return;

            InformationElement[] elems = sets[0];
            double value = ((IeShortFloat) elems[0]).getValue();
            if (!Double.isFinite(value)) {
                log.warn("Ignoring non-finite IEC measurement for IOA {}: {}", ioa, value);
                return;
            }

            Instant currentTime = Instant.now();
            Measurement m = new Measurement(null, currentTime, value);

            measurementQueue.add(new ReceivedMeasurement(ioa, m));
        });
    }

    private record ReceivedMeasurement(
            int ioa,
            Measurement measurement
    ) {}
}
