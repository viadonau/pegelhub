package at.pegelhub.connector.iec.iec.impl;

import at.pegelhub.connector.iec.datapoints.IecMappingIndex;
import at.pegelhub.connector.iec.jobs.IecToCoreJob;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.model.Measurement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openmuc.j60870.*;
import org.openmuc.j60870.ie.IeQuality;
import org.openmuc.j60870.ie.IeShortFloat;
import org.openmuc.j60870.ie.InformationObject;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

class IecRecoveryIntegrationTest {
    @Test
    @Timeout(20)
    void deliversMeasurementsAfterLateServerStartAndServerRestart() throws Exception {
        int port;
        var loopback = InetAddress.getByName("127.0.0.1");
        try (var reservation = new ServerSocket(0, 1, loopback)) {
            port = reservation.getLocalPort();
        }
        var client = new IecClientImpl("127.0.0.1", port, 514, Set.of(100));
        var core = mock(PegelHubClient.class);
        var mappings = mock(IecMappingIndex.class);
        UUID timeSeriesId = UUID.randomUUID();
        when(mappings.getTimeSeriesId(100)).thenReturn(Optional.of(timeSeriesId));
        var job = new IecToCoreJob(client, mappings, core);
        var accepted = new CopyOnWriteArrayList<Connection>();
        var server = Server.builder().setBindAddr(loopback).setPort(port).build();
        var listener = mock(ServerEventListener.class);
        var data = mock(ConnectionEventListener.class);
        doAnswer(invocation -> {
            Connection connection = invocation.getArgument(0);
            connection.setConnectionListener(data);
            accepted.add(connection);
            return null;
        }).when(listener).connectionIndication(any());

        try {
            // First attempt really fails against a port with no IEC server.
            client.connect();
            assertThatThrownBy(() -> client.sendMeasurement(100, new Measurement(null, Instant.now(), 1.0)))
                    .isInstanceOf(IllegalStateException.class);
            server.start(listener);
            client.connect();
            await().atMost(Duration.ofSeconds(3)).until(() -> accepted.size() == 1);
            accepted.getFirst().send(reading(Float.NaN));
            accepted.getFirst().send(reading(1));
            await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
                job.run();
                verify(core).sendMeasurements(argThat(batch -> containsReading(batch, timeSeriesId, 1.0)));
            });

            server.stop();
            accepted.forEach(Connection::close);
            await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                    assertThatThrownBy(() -> client.sendMeasurement(100, new Measurement(null, Instant.now(), 1.0)))
                            .isInstanceOf(IllegalStateException.class));
            client.connect();
            server.start(listener);
            client.connect();
            await().atMost(Duration.ofSeconds(3)).until(() -> accepted.size() == 2);
            accepted.getLast().send(reading(Float.NaN));
            accepted.getLast().send(reading(2));
            await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
                job.run();
                verify(core).sendMeasurements(argThat(batch -> containsReading(batch, timeSeriesId, 2.0)));
            });
            verify(core, times(2)).sendMeasurements(anyList());
        } finally {
            client.disconnect();
            server.stop();
            accepted.forEach(Connection::close);
        }
    }

    private static boolean containsReading(List<Measurement> batch, UUID timeSeriesId, double value) {
        return batch.size() == 1 && batch.getFirst().getTimeSeriesId().equals(timeSeriesId)
                && batch.getFirst().getValue() == value;
    }

    private static ASdu reading(float value) {
        return new ASdu(ASduType.M_ME_NC_1, false, CauseOfTransmission.SPONTANEOUS, false, false, 0, 514,
                new InformationObject(100, new IeShortFloat(value), new IeQuality(false, false, false, false, false)));
    }
}
