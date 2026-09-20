package at.pegelhub.lib.runtime;

import at.pegelhub.connector.iec.app.IecConnectorModule;
import at.pegelhub.connector.iec.iec.impl.IecClientImpl;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.PegelHubClientFactory;
import at.pegelhub.lib.config.ConnectorConfigDirectory;
import at.pegelhub.lib.model.Measurement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class IecRuntimeDefinitionTest {
    @Test
    void schedulesImmediateFixedDelayRecoveryAlongsideBothPollingWorkers() throws Exception {
        var factory = mock(PegelHubClientFactory.class);
        when(factory.create(any())).thenReturn(mock(PegelHubClient.class));
        try (var clients = mockConstruction(IecClientImpl.class)) {
            var definition = new IecConnectorModule().define(
                    ConnectorConfigDirectory.at(Path.of("examples/config")), factory);
            assertThat(definition.threadCount()).isEqualTo(3);
            assertThat(definition.startActions()).isEmpty();
            assertThat(definition.tasks()).extracting(ConnectorRuntimeDefinition.ScheduledTask::name)
                    .containsExactly("iec-reconnect", "iec-to-core", "core-to-iec");
            var recovery = definition.tasks().getFirst();
            assertThat(recovery.initialDelay()).isEqualTo(Duration.ZERO);
            assertThat(recovery.delay()).isEqualTo(Duration.ofSeconds(10));
            for (var task : definition.tasks().subList(1, 3)) {
                assertThat(task.initialDelay()).isEqualTo(Duration.ofSeconds(1));
                assertThat(task.delay()).isEqualTo(Duration.ofSeconds(30));
            }
            recovery.runnable().run();
            verify(clients.constructed().getFirst()).connect();
            for (var resource : definition.resources().reversed()) {
                resource.close();
            }
            verify(clients.constructed().getFirst()).disconnect();
        }
    }

    @Test
    void latestPolicyUsesSharedPollingScheduleAndSelectsLastReading(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("connector.yaml"),
                Files.readString(Path.of("examples/config/connector.yaml")).replace("\"30s\"", "\"5m\"") + """
                ingestion:
                  mode: latest
                """);
        Files.createDirectories(directory.resolve("mappings"));
        Files.writeString(directory.resolve("mappings/in.yaml"), """
                iecIoa: 42
                timeSeriesId: "11111111-1111-1111-1111-111111111111"
                direction: external-to-core
                """);
        var factory = mock(PegelHubClientFactory.class);
        var core = mock(PegelHubClient.class);
        when(factory.create(any())).thenReturn(core);
        try (var clients = mockConstruction(IecClientImpl.class)) {
            var definition = new IecConnectorModule().define(ConnectorConfigDirectory.at(directory), factory);
            try {
                var tasks = definition.tasks();
                assertThat(tasks.get(0).delay()).isEqualTo(Duration.ofSeconds(10));
                assertThat(tasks.get(1).initialDelay()).isEqualTo(Duration.ofSeconds(1));
                assertThat(tasks.get(1).delay()).isEqualTo(Duration.ofMinutes(5));
                assertThat(tasks.get(2).initialDelay()).isEqualTo(Duration.ofSeconds(1));
                assertThat(tasks.get(2).delay()).isEqualTo(Duration.ofMinutes(5));
                when(clients.constructed().getFirst().drainGroupedMeasurements()).thenReturn(Map.of(42,
                        List.of(new Measurement(null, Instant.EPOCH, 1.0),
                                new Measurement(null, Instant.EPOCH.plusSeconds(1), 2.0))));
                tasks.get(1).runnable().run();
                verify(core).sendMeasurements(argThat(values -> values.size() == 1
                        && values.getFirst().getValue().equals(2.0)));
            } finally {
                for (var resource : definition.resources().reversed()) resource.close();
            }
        }
    }
}
