package at.pegelhub.lib.runtime;

import at.pegelhub.connector.iec.app.IecConnectorModule;
import at.pegelhub.connector.iec.iec.impl.IecClientImpl;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.PegelHubClientFactory;
import at.pegelhub.lib.config.ConnectorConfigDirectory;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;

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
            recovery.runnable().run();
            verify(clients.constructed().getFirst()).connect();
            for (var resource : definition.resources().reversed()) {
                resource.close();
            }
            verify(clients.constructed().getFirst()).disconnect();
        }
    }
}
