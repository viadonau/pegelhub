package at.pegelhub.lib.runtime;

import at.pegelhub.connector.icc.IccConnectorModule;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.config.ConnectorConfigDirectory;
import at.pegelhub.lib.model.Measurement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IccRuntimeTest {
    @TempDir
    Path directory;

    @Test
    void retriesOneDirectionWhileTheOtherAdvancesAndClosesBothClients() throws Exception {
        UUID localId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID remoteId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        Instant observedAt = Instant.now().minusSeconds(30);
        writeConfiguration(localId, remoteId);
        var local = mock(PegelHubClient.class);
        var remote = mock(PegelHubClient.class);
        var outboundReads = new CopyOnWriteArrayList<ReadWindow>();
        var inboundReads = new CopyOnWriteArrayList<ReadWindow>();
        when(local.getMeasurementsOfTimeSeries(eq(localId), any(), any())).thenAnswer(call -> {
            outboundReads.add(new ReadWindow(call.getArgument(1), call.getArgument(2)));
            return List.of(new Measurement(localId, observedAt, 42));
        });
        when(remote.getMeasurementsOfTimeSeries(eq(remoteId), any(), any())).thenAnswer(call -> {
            inboundReads.add(new ReadWindow(call.getArgument(1), call.getArgument(2)));
            return List.of(new Measurement(remoteId, observedAt, 7));
        });
        doThrow(new IllegalStateException("Remote unavailable")).doNothing().when(remote).sendMeasurements(anyList());
        var inboundDeliveries = new CountDownLatch(2);
        doAnswer(call -> { inboundDeliveries.countDown(); return null; }).when(local).sendMeasurements(anyList());
        var definition = new IccConnectorModule().define(ConnectorConfigDirectory.at(directory),
                connection -> connection.authentication().clientId().equals("local") ? local : remote);

        try (var runtime = ConnectorRuntime.start(definition)) {
            assertTrue(inboundDeliveries.await(5, TimeUnit.SECONDS), "Both polls should run automatically");
        }

        ReadWindow first = outboundReads.getFirst();
        assertEquals(first, inboundReads.getFirst());
        assertEquals(first.until().minusSeconds(3601), first.from());
        assertEquals(first.from(), outboundReads.get(1).from());
        assertEquals(first.until().minusSeconds(3600), inboundReads.get(1).from());
        assertEquals(outboundReads.get(1).until(), inboundReads.get(1).until());
        verify(remote, atLeast(2)).sendMeasurements(argThat(batch -> batch.size() == 1
                && batch.getFirst().getTimeSeriesId().equals(remoteId)
                && batch.getFirst().getObservedAt().equals(observedAt) && batch.getFirst().getValue() == 42));
        verify(local, atLeast(2)).sendMeasurements(argThat(batch -> batch.size() == 1
                && batch.getFirst().getTimeSeriesId().equals(localId) && batch.getFirst().getValue() == 7));
        verify(local).close();
        verify(remote).close();
    }

    private void writeConfiguration(UUID localId, UUID remoteId) throws Exception {
        Files.writeString(directory.resolve("connector.yaml"), """
                localCore:
                  baseUrl: http://local.invalid/
                  authentication: { tokenUrl: 'http://local.invalid/token', clientId: local, clientSecret: test }
                remoteCore:
                  baseUrl: http://remote.invalid/
                  authentication: { tokenUrl: 'http://remote.invalid/token', clientId: remote, clientSecret: test }
                polling:
                  interval: 1s
                """);
        Files.createDirectory(directory.resolve("mappings"));
        Files.writeString(directory.resolve("mappings/1.yaml"), """
                timeSeriesId: %s
                externalTimeSeriesId: %s
                direction: core-to-external
                """.formatted(localId, remoteId));
        Files.writeString(directory.resolve("mappings/2.yaml"), """
                timeSeriesId: %s
                externalTimeSeriesId: %s
                direction: external-to-core
                """.formatted(localId, remoteId));
    }

    private record ReadWindow(Instant from, Instant until) {}
}
