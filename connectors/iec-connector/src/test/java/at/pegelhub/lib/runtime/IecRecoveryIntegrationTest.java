package at.pegelhub.lib.runtime;

import at.pegelhub.connector.iec.iec.impl.IecClientImpl;

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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

class IecRecoveryIntegrationTest {
    @Test
    @Timeout(60)
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

        var attempts = new AtomicInteger();
        var assembly = ConnectorRuntimeAssembly.begin("iec-recovery-test").threadCount(3);
        assembly.own(client::disconnect);
        assembly.fixedDelayTask("iec-reconnect", () -> {
            client.connect();
            attempts.incrementAndGet();
        }, Duration.ofSeconds(10));
        assembly.fixedDelayTask("iec-to-core", job, Duration.ofMillis(20));
        try (var runtime = ConnectorRuntime.start(assembly.complete())) {
            // A real scheduled attempt fails before the local server starts.
            await().atMost(Duration.ofSeconds(3)).until(() -> attempts.get() == 1);
            assertThatThrownBy(() -> client.sendMeasurement(100, new Measurement(null, Instant.now(), 1.0)))
                    .isInstanceOf(IllegalStateException.class);
            server.start(listener);
            await().atMost(Duration.ofSeconds(13)).until(() -> accepted.size() == 1 && !accepted.getFirst().isStopped());
            accepted.getFirst().send(reading(Float.NaN));
            accepted.getFirst().send(reading(1));
            await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
                verify(core).sendMeasurements(argThat(batch -> containsReading(batch, timeSeriesId, 1.0)));
            });

            server.stop();
            accepted.forEach(Connection::close);
            await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                    assertThatThrownBy(() -> client.sendMeasurement(100, new Measurement(null, Instant.now(), 1.0)))
                            .isInstanceOf(IllegalStateException.class));
            int beforeOfflineAttempt = attempts.get();
            await().atMost(Duration.ofSeconds(13)).until(() -> attempts.get() > beforeOfflineAttempt);
            server.start(listener);
            await().atMost(Duration.ofSeconds(13)).until(() -> accepted.size() == 2 && !accepted.getLast().isStopped());
            accepted.getLast().send(reading(Float.NaN));
            accepted.getLast().send(reading(2));
            await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
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
