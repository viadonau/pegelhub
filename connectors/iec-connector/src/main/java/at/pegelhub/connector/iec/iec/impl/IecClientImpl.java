package at.pegelhub.connector.iec.iec.impl;

import at.pegelhub.connector.iec.iec.IecClient;
import at.pegelhub.lib.model.Measurement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openmuc.j60870.*;
import org.openmuc.j60870.ie.*;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class IecClientImpl implements IecClient {
    private volatile Connection connection;
    private final String host;
    private final int port;
    private final int commonAddress;
    private final Set<Integer> registeredProtocolToCoreIoas;
    private final BlockingQueue<ReceivedMeasurement> measurementQueue = new LinkedBlockingQueue<>();
    private final Object lifecycle = new Object();
    private volatile boolean stopped;
    private boolean connecting;

    @Override
    public void connect() {
        Connection previous;
        synchronized (lifecycle) {
            if (stopped || connecting || Thread.currentThread().isInterrupted()
                    || (connection != null && !connection.isClosed() && !connection.isStopped())) {
                return;
            }
            connecting = true;
            previous = connection;
            connection = null;
        }

        Connection candidate = null;
        try {
            if (previous != null) {
                previous.close();
            }
            log.info("Attempting IEC connection to {}:{}", host, port);
            candidate = new ClientConnectionBuilder(InetAddress.getByName(host)).setPort(port).build();
            if (stopped || Thread.currentThread().isInterrupted()) {
                return;
            }
            candidate.startDataTransfer(createIecListener());
            // j60870 can return from STARTDT with the interrupt flag set.
            if (stopped || Thread.currentThread().isInterrupted()) {
                return;
            }
            candidate.interrogation(commonAddress, CauseOfTransmission.ACTIVATION, new IeQualifierOfInterrogation(20));
            if (candidate.isClosed() || candidate.isStopped()) {
                throw new IOException("IEC connection closed or stopped during startup");
            }
            synchronized (lifecycle) {
                if (stopped || Thread.currentThread().isInterrupted()) {
                    return;
                }
                connection = candidate;
                candidate = null;
            }
            log.info("Connected to IEC server at {}:{}", host, port);
        } catch (IOException e) {
            log.warn("IEC connection failed; a later scheduled attempt will retry: {}", e.toString());
        } finally {
            try {
                if (candidate != null) {
                    candidate.close();
                }
            } finally {
                synchronized (lifecycle) {
                    connecting = false;
                }
            }
        }
    }

    @Override
    public void disconnect() {
        Connection previous;
        synchronized (lifecycle) {
            stopped = true;
            previous = connection;
            connection = null;
        }
        if (previous != null) {
            previous.close();
            log.info("Disconnected from IEC server");
        }
    }

    @Override
    public void sendMeasurement(int ioa, Measurement measurement) {
        Connection current = connection;
        if (stopped || current == null || current.isClosed() || current.isStopped()) {
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
            current.send(asdu);
        } catch (IOException e) {
            current.close();
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

    private ConnectionEventListener createIecListener() {
        return new ConnectionEventListener() {
            @Override
            public void newASdu(ASdu aSdu) {
                log.info("Received ASDU: {}", aSdu);
                enqueueMeasurements(aSdu);
            }

            @Override
            public void connectionClosed(IOException e) {
                log.info("Received connection closed signal: {}", e != null ? e.getMessage() : "(no message)");
            }

            @Override
            public void dataTransferStateChanged(boolean stopped) {
                log.info("Data transfer started: {}", stopped ? "stopped" : "started");
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
