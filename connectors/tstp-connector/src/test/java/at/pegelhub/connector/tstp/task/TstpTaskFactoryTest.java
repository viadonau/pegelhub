package at.pegelhub.connector.tstp.task;

import at.pegelhub.connector.tstp.TstpMapping;
import at.pegelhub.connector.tstp.communication.impl.TstpCommunicatorImpl;
import at.pegelhub.connector.tstp.config.TstpConnectorConfig;
import at.pegelhub.connector.tstp.config.TstpServer;
import at.pegelhub.connector.tstp.service.impl.TstpCatalogServiceImpl;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.config.CoreAuthentication;
import at.pegelhub.lib.config.CoreConnection;
import at.pegelhub.lib.config.MappingDirection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;

import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TstpTaskFactoryTest {
    private static final UUID TIME_SERIES_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void createsReaderForProtocolToCoreMapping() throws Exception {
        PegelHubClient coreClient = mock(PegelHubClient.class);
        try (var communicatorMock = mockConstruction(TstpCommunicatorImpl.class);
             var catalogMock = mockConstruction(TstpCatalogServiceImpl.class);
             var readerMock = mockConstruction(TstpReader.class);
             var writerMock = mockConstruction(TstpWriter.class)) {

            TstpRuntimeTask task = TstpTaskFactory.getTstpTask(
                    configFor(MappingDirection.EXTERNAL_TO_CORE), coreClient);

            assertEquals(1, communicatorMock.constructed().size());
            assertEquals(1, catalogMock.constructed().size());
            assertEquals(1, readerMock.constructed().size());
            assertEquals(0, writerMock.constructed().size());
            assertSame(readerMock.constructed().getFirst(), task.task());
            assertSame(readerMock.constructed().getFirst(), task.closeable());
        }
    }

    @Test
    void createsWriterForCoreToProtocolMapping() throws Exception {
        PegelHubClient coreClient = mock(PegelHubClient.class);
        try (var communicatorMock = mockConstruction(TstpCommunicatorImpl.class);
             var catalogMock = mockConstruction(TstpCatalogServiceImpl.class);
             var readerMock = mockConstruction(TstpReader.class);
             var writerMock = mockConstruction(TstpWriter.class)) {

            TstpRuntimeTask task = TstpTaskFactory.getTstpTask(
                    configFor(MappingDirection.CORE_TO_EXTERNAL), coreClient);

            assertEquals(1, communicatorMock.constructed().size());
            assertEquals(1, catalogMock.constructed().size());
            assertEquals(0, readerMock.constructed().size());
            assertEquals(1, writerMock.constructed().size());
            assertSame(writerMock.constructed().getFirst(), task.task());
            assertSame(writerMock.constructed().getFirst(), task.closeable());
        }
    }

    @Test
    void doesNotRefreshCatalogDuringTaskConstruction() throws Exception {
        PegelHubClient coreClient = mock(PegelHubClient.class);
        try (var communicatorMock = mockConstruction(TstpCommunicatorImpl.class);
             var readerMock = mockConstruction(TstpReader.class)) {

            TstpTaskFactory.getTstpTask(configFor(MappingDirection.EXTERNAL_TO_CORE), coreClient);

            verify(communicatorMock.constructed().getFirst(), never()).getCatalog(77);
            assertEquals(1, readerMock.constructed().size());
        }
    }

    private TstpConnectorConfig configFor(MappingDirection direction) throws Exception {
        return new TstpConnectorConfig(
                new CoreConnection(
                        URI.create("http://core.local/").toURL(),
                        new CoreAuthentication("http://keycloak.local/token", "connector", "secret")),
                new TstpServer("127.0.0.2", 8030),
                Duration.ofSeconds(10),
                new TstpMapping(TIME_SERIES_ID, 77, direction));
    }
}
