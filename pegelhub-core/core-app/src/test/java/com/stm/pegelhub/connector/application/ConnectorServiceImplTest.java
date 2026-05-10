package com.stm.pegelhub.connector.application;

import com.stm.pegelhub.connector.domain.Connector;
import com.stm.pegelhub.connector.persistence.ConnectorRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static com.stm.pegelhub.testsupport.ExampleData.CONNECTOR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

final class ConnectorServiceImplTest {

    private ConnectorServiceImpl connectorService;
    private static final ConnectorRepository REPOSITORY = mock(ConnectorRepository.class);

    @BeforeEach
    public void prepare() {
        connectorService = new ConnectorServiceImpl(REPOSITORY);
        reset(REPOSITORY);
    }

    @Test
    public void constructorWithNullArgsThrowsNPE() {
        assertThrows(NullPointerException.class, () -> new ConnectorServiceImpl(null));
    }

    @Test
    public void createConnector() {
        when(REPOSITORY.saveConnector(any())).thenReturn(CONNECTOR);

        Connector result = connectorService.createConnector(CONNECTOR);
        assertEquals(CONNECTOR, result);
        verify(REPOSITORY, times(1)).saveConnector(any());
    }

    @Test
    public void getById() {
        when(REPOSITORY.getById(any())).thenReturn(CONNECTOR);

        Connector result = connectorService.getConnectorById(UUID.randomUUID());
        assertEquals(CONNECTOR, result);
        verify(REPOSITORY, times(1)).getById(any());
    }


    @Test
    public void getAll() {
        when(REPOSITORY.getAllConnectors()).thenReturn(List.of(CONNECTOR));

        List<Connector> result = connectorService.getAllConnectors();
        assertEquals(1, result.size());
        Assertions.assertThat(result).containsOnly(CONNECTOR);
        verify(REPOSITORY, times(1)).getAllConnectors();
    }
}