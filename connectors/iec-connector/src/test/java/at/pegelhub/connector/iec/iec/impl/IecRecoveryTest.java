package at.pegelhub.connector.iec.iec.impl;

import at.pegelhub.lib.model.Measurement;
import org.junit.jupiter.api.Test;
import org.openmuc.j60870.*;
import org.openmuc.j60870.ie.*;

import java.io.IOException;
import java.net.Socket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class IecRecoveryTest {
    @Test
    void retriesBeyondTenFailuresAndClosesFailedTransports() throws Exception {
        var healthy = mock(Connection.class);
        var sockets = new ArrayList<Socket>();
        var client = client((host, port, socket) -> {
            sockets.add(socket);
            if (sockets.size() <= 12) throw new IOException("offline or DNS unavailable");
            return healthy;
        });
        try {
            for (int i = 0; i < 14; i++) client.connect();
            assertThat(sockets).hasSize(13);
            assertThat(sockets.subList(0, 12)).allMatch(Socket::isClosed);
            assertThat(sockets.getLast().isClosed()).isFalse();
            verify(healthy).startDataTransfer(any());
            verify(healthy).interrogation(eq(514), any(), any());
        } finally {
            client.disconnect();
        }
        assertThat(sockets).allMatch(Socket::isClosed);
    }

    @Test
    void rejectsStaleCallbacksButAcceptsInitialCandidateData() throws Exception {
        var first = mock(Connection.class);
        var second = mock(Connection.class);
        var oldListener = new AtomicReference<ConnectionEventListener>();
        var newListener = new AtomicReference<ConnectionEventListener>();
        doAnswer(call -> {
            oldListener.set(call.getArgument(0));
            oldListener.get().newASdu(reading(1));
            return null;
        }).when(first).startDataTransfer(any());
        doAnswer(call -> {
            newListener.set(call.getArgument(0));
            return null;
        }).when(second).startDataTransfer(any());
        var opener = mock(IecConnectionOpener.class);
        when(opener.open(anyString(), anyInt(), any())).thenReturn(first, second);
        var client = client(opener);
        try {
            client.connect();
            oldListener.get().dataTransferStateChanged(true);
            client.connect();
            oldListener.get().newASdu(reading(99));
            oldListener.get().connectionClosed(new IOException("late"));
            oldListener.get().dataTransferStateChanged(true);
            newListener.get().newASdu(reading(2));
            client.connect();
            verify(opener, times(2)).open(anyString(), anyInt(), any());
            assertThat(client.drainGroupedMeasurements().get(100))
                    .extracting(Measurement::getValue).containsExactly(1.0, 2.0);
            client.sendMeasurement(100, measurement());
            verify(second).send(any());
        } finally {
            client.disconnect();
        }
        newListener.get().newASdu(reading(3));
        assertThat(client.drainGroupedMeasurements()).isEmpty();
        client.connect();
        verify(opener, times(2)).open(anyString(), anyInt(), any());
    }

    @Test
    void closesFailedStartDtAndInterrogationThenRecovers() throws Exception {
        var startFailure = mock(Connection.class);
        var interrogationFailure = mock(Connection.class);
        var healthy = mock(Connection.class);
        doThrow(new IOException("STARTDT timeout")).when(startFailure).startDataTransfer(any());
        doThrow(new IOException("interrogation failure")).when(interrogationFailure).interrogation(anyInt(), any(), any());
        var opener = mock(IecConnectionOpener.class);
        when(opener.open(anyString(), anyInt(), any())).thenReturn(startFailure, interrogationFailure, healthy);
        var client = client(opener);
        try {
            client.connect();
            client.connect();
            client.connect();
            verify(startFailure).close();
            verify(interrogationFailure).close();
            client.sendMeasurement(100, measurement());
            verify(healthy).send(any());
        } finally {
            client.disconnect();
        }
    }

    @Test
    void doesNotPublishInterruptedHandshake() throws Exception {
        var connection = mock(Connection.class);
        doAnswer(call -> { Thread.currentThread().interrupt(); return null; })
                .when(connection).startDataTransfer(any());
        var client = client((host, port, socket) -> connection);
        try {
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
    void shutdownClosesSocketDuringBuildAndRejectsLateResult() throws Exception {
        assertShutdownDuringAttempt(false);
    }

    @Test
    void shutdownClosesSocketDuringHandshakeAndPreventsConcurrentAttempts() throws Exception {
        assertShutdownDuringAttempt(true);
    }

    private void assertShutdownDuringAttempt(boolean handshake) throws Exception {
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var connection = mock(Connection.class);
        var transport = new AtomicReference<Socket>();
        var opener = mock(IecConnectionOpener.class);
        when(opener.open(anyString(), anyInt(), any())).thenAnswer(call -> {
            transport.set(call.getArgument(2));
            if (!handshake) {
                entered.countDown();
                assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
            }
            return connection;
        });
        if (handshake) doAnswer(call -> {
            entered.countDown();
            assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
            return null;
        }).when(connection).startDataTransfer(any());
        var client = client(opener);
        try (var executor = Executors.newSingleThreadExecutor()) {
            var attempt = executor.submit(client::connect);
            try {
                assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
                client.connect();
                client.disconnect();
                assertThat(transport.get().isClosed()).isTrue();
            } finally {
                release.countDown();
            }
            attempt.get(5, TimeUnit.SECONDS);
            verify(opener).open(anyString(), anyInt(), any());
            verify(connection, atLeastOnce()).close();
            verify(connection, never()).interrogation(anyInt(), any(), any());
            assertThatThrownBy(() -> client.sendMeasurement(100, measurement())).isInstanceOf(IllegalStateException.class);
        } finally {
            client.disconnect();
        }
    }

    @Test
    void failedSendReturnsToScheduledRecovery() throws Exception {
        var first = mock(Connection.class);
        var second = mock(Connection.class);
        doThrow(new IOException("broken socket")).when(first).send(any());
        var opener = mock(IecConnectionOpener.class);
        when(opener.open(anyString(), anyInt(), any())).thenReturn(first, second);
        var client = client(opener);
        try {
            client.connect();
            assertThatThrownBy(() -> client.sendMeasurement(100, measurement())).isInstanceOf(IllegalStateException.class);
            client.connect();
            client.sendMeasurement(100, measurement());
            verify(second).send(any());
        } finally {
            client.disconnect();
        }
    }

    private static IecClientImpl client(IecConnectionOpener opener) {
        return new IecClientImpl("127.0.0.1", 2404, 514, Set.of(100), opener);
    }

    private static Measurement measurement() { return new Measurement(null, Instant.now(), 1.0); }

    private static ASdu reading(float value) {
        return new ASdu(ASduType.M_ME_NC_1, false, CauseOfTransmission.SPONTANEOUS, false, false, 0, 514,
                new InformationObject(100, new IeShortFloat(value), new IeQuality(false, false, false, false, false)));
    }
}
