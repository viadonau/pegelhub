package at.pegelhub.connector.iec.iec.impl;

import at.pegelhub.lib.model.Measurement;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.openmuc.j60870.ClientConnectionBuilder;
import org.openmuc.j60870.Connection;
import org.openmuc.j60870.ConnectionEventListener;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class IecRecoveryTest {
    private final IecClientImpl client = new IecClientImpl("iec.example", 2404, 514, Set.of(100));

    @Test
    void retriesBeyondTenFailuresThenRetainsHealthyConnection() throws Exception {
        var connection = mock(Connection.class);
        var loopback = InetAddress.getLoopbackAddress();
        try (var dns = mockStatic(InetAddress.class);
             var builders = mockConstruction(ClientConnectionBuilder.class, withSettings().defaultAnswer(RETURNS_SELF),
                     (builder, context) -> {
                         if (context.getCount() <= 12) {
                             when(builder.build()).thenThrow(new IOException("offline"));
                         } else {
                             when(builder.build()).thenReturn(connection);
                         }
                     })) {
            dns.when(() -> InetAddress.getByName("iec.example")).thenReturn(loopback);
            for (int i = 0; i < 14; i++) {
                client.connect();
            }
            assertThat(builders.constructed()).hasSize(13);
            dns.verify(() -> InetAddress.getByName("iec.example"), times(13));
            verify(connection).startDataTransfer(any());
            verify(connection).interrogation(eq(514), any(), any());
            verify(connection, never()).close();
            client.disconnect();
        }
    }

    @Test
    void retriesDnsResolutionAndRecovers() throws Exception {
        var connection = mock(Connection.class);
        var loopback = InetAddress.getLoopbackAddress();
        try (var dns = mockStatic(InetAddress.class); var builders = connections(connection)) {
            dns.when(() -> InetAddress.getByName("iec.example"))
                    .thenThrow(new UnknownHostException("temporary DNS failure")).thenReturn(loopback);
            client.connect();
            assertThat(builders.constructed()).isEmpty();
            client.connect();
            assertThat(builders.constructed()).hasSize(1);
            verify(connection).startDataTransfer(any());
            client.disconnect();
        }
    }

    @Test
    void replacesStoppedConnectionAndIgnoresOldCallbacks() throws Exception {
        var first = mock(Connection.class);
        var second = mock(Connection.class);
        var client = localClient();
        try (var builders = connections(first, second)) {
            client.connect();
            var listener = ArgumentCaptor.forClass(ConnectionEventListener.class);
            verify(first).startDataTransfer(listener.capture());
            when(first.isStopped()).thenReturn(true);
            client.connect();
            listener.getValue().connectionClosed(new IOException("late callback"));
            listener.getValue().dataTransferStateChanged(true);
            client.connect();
            assertThat(builders.constructed()).hasSize(2);
            verify(first).close();
            verify(second, never()).close();
            client.sendMeasurement(100, measurement());
            verify(second).send(any());
            client.disconnect();
            client.connect();
            assertThat(builders.constructed()).hasSize(2);
            verify(second).close();
        }
    }

    @Test
    void closesFailedStartDtAndInterrogationCandidatesThenRecovers() throws Exception {
        var startFailure = mock(Connection.class);
        var interrogationFailure = mock(Connection.class);
        var healthy = mock(Connection.class);
        doThrow(new IOException("STARTDT timeout")).when(startFailure).startDataTransfer(any());
        doThrow(new IOException("interrogation failure")).when(interrogationFailure).interrogation(anyInt(), any(), any());
        var client = localClient();
        try (var builders = connections(startFailure, interrogationFailure, healthy)) {
            client.connect();
            client.connect();
            client.connect();
            verify(startFailure).close();
            verify(interrogationFailure).close();
            verify(healthy, never()).close();
            client.sendMeasurement(100, measurement());
            verify(healthy).send(any());
            client.disconnect();
        }
    }

    @Test
    void doesNotPublishAnInterruptedHandshake() throws Exception {
        var connection = mock(Connection.class);
        doAnswer(invocation -> {
            Thread.currentThread().interrupt();
            return null;
        }).when(connection).startDataTransfer(any());
        var client = localClient();
        try (var builders = connections(connection)) {
            client.connect();
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            verify(connection).close();
            verify(connection, never()).interrogation(anyInt(), any(), any());
            assertThatThrownBy(() -> client.sendMeasurement(100, measurement())).isInstanceOf(IllegalStateException.class);
        } finally {
            Thread.interrupted();
            client.disconnect();
        }
    }

    @Test
    void shutdownDoesNotWaitForHandshakeOrAllowConcurrentAttempts() throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var connection = mock(Connection.class);
        doAnswer(invocation -> {
            entered.countDown();
            assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
            return null;
        }).when(connection).startDataTransfer(any());
        var client = localClient();
        try (var executor = Executors.newSingleThreadExecutor()) {
            var attempt = executor.submit(() -> {
                try (var builders = connections(connection)) {
                    client.connect();
                    assertThat(builders.constructed()).hasSize(1);
                }
                return null;
            });
            try {
                assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
                client.connect();
                client.disconnect();
            } finally {
                release.countDown();
            }
            attempt.get(5, TimeUnit.SECONDS);
            verify(connection).close();
            verify(connection, never()).interrogation(anyInt(), any(), any());
            assertThatThrownBy(() -> client.sendMeasurement(100, measurement())).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void failedSendClosesConnectionAndNextAttemptReplacesIt() throws Exception {
        var first = mock(Connection.class);
        var second = mock(Connection.class);
        doAnswer(invocation -> {
            when(first.isClosed()).thenReturn(true);
            return null;
        }).when(first).close();
        doThrow(new IOException("broken socket")).when(first).send(any());
        var client = localClient();
        assertThatThrownBy(() -> client.sendMeasurement(100, measurement())).isInstanceOf(IllegalStateException.class);
        try (var builders = connections(first, second)) {
            client.connect();
            assertThatThrownBy(() -> client.sendMeasurement(100, measurement()))
                    .isInstanceOf(IllegalStateException.class).hasCauseInstanceOf(IOException.class);
            verify(first).close();
            client.connect();
            client.sendMeasurement(100, measurement());
            verify(second).send(any());
            client.disconnect();
        }
    }

    private static IecClientImpl localClient() {
        return new IecClientImpl("127.0.0.1", 2404, 514, Set.of(100));
    }

    private static Measurement measurement() {
        return new Measurement(null, Instant.now(), 1.0);
    }

    private static MockedConstruction<ClientConnectionBuilder> connections(Connection... connections) {
        return mockConstruction(ClientConnectionBuilder.class, withSettings().defaultAnswer(RETURNS_SELF),
                (builder, context) -> when(builder.build()).thenReturn(connections[context.getCount() - 1]));
    }
}
