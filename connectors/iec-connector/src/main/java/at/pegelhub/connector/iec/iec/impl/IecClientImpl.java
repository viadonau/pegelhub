package at.pegelhub.connector.iec.iec.impl;

import at.pegelhub.connector.iec.iec.IecClient;
import at.pegelhub.lib.model.Measurement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openmuc.j60870.*;
import org.openmuc.j60870.ie.*;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class IecClientImpl implements IecClient {
    private Connection connection;
    private final InetAddress host;
    private final int port;
    private final int commonAddress;
    private final Set<Integer> registeredSupplierIoas;
    private final BlockingQueue<ReceivedMeasurement> measurementQueue = new LinkedBlockingQueue<>();

    @Override
    public void connect() {
        if (this.connection != null) {
            try {
                this.connection.close();
            } catch (Exception ignore) {
            }
            this.connection = null;
        }

        for (int attempt = 1; attempt <= 10; attempt++) {
            log.info("Attempting to rebuild IEC connection (attempt {}/{})", attempt, 10);
            try {
                Connection c = new ClientConnectionBuilder(host)
                        .setPort(port)
                        .build();

                c.startDataTransfer(createIecListener());
                c.interrogation(this.commonAddress, CauseOfTransmission.ACTIVATION, new IeQualifierOfInterrogation(20));

                this.connection = c;
                log.info("Connected to IEC server at {}:{}", host, port);
                return;
            } catch (IOException e) {
                log.warn("Failed to connect: {}", e.toString());
                if (attempt == 10) {
                    throw new RuntimeException("Quit reconnecting to IEC-Server", e);
                }
                try {
                    Thread.sleep(Duration.ofSeconds(10).toMillis());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    @Override
    public void disconnect() {
        if (connection != null) {
            connection.close();
            log.info("Disconnected from IEC server");
        }
    }

    @Override
    public void sendMeasurement(int ioa, Measurement measurement) {
        float value = measurement.getFields().get("value").floatValue();

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
            connection.send(asdu);
        } catch (IOException e) {
            log.error("Error while sending ASDU: {}", e.getMessage());
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
                connect();
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

            if (!registeredSupplierIoas.contains(ioa)) {
                log.debug("Ignoring IOA {} (no supplier registered).", ioa);
                return;
            }

            InformationElement[][] sets = io.getInformationElements();
            if (sets.length == 0) return;

            InformationElement[] elems = sets[0];
            double value = ((IeShortFloat) elems[0]).getValue();

            Map<String, Double> fields = new HashMap<>();
            fields.put("value", value);

            LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.UTC);
            Measurement m = new Measurement(currentTime, fields, new HashMap<>());

            measurementQueue.add(new ReceivedMeasurement(ioa, m));
        });
    }

    private record ReceivedMeasurement(int ioa, Measurement measurement) {}
}