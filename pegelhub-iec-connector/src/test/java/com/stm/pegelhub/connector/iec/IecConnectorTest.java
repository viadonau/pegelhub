package com.stm.pegelhub.connector.iec;

import com.stm.pegelhub.lib.PegelHubCommunicator;
import com.stm.pegelhub.lib.PegelHubCommunicatorFactory;
import com.stm.pegelhub.lib.model.Measurement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

class IecConnectorTest {

    private IecConnector connector;
    private PegelHubCommunicator mockCommunicator;
    private MockedStatic<PegelHubCommunicatorFactory> communicatorFactoryMock;

    @BeforeEach
    void setUp() throws Exception {
        mockCommunicator = mock(PegelHubCommunicator.class);
        communicatorFactoryMock = Mockito.mockStatic(PegelHubCommunicatorFactory.class);
        communicatorFactoryMock.when(() -> PegelHubCommunicatorFactory.create(any(), any()))
                .thenReturn(mockCommunicator);

        String configPath = "src/test/resources/ConnectorTest.properties";
        ConnectorOptions options = ConfigLoader.readArguments(new String[]{configPath});

        when(mockCommunicator.getSystemTime())
                .thenReturn(Timestamp.valueOf(LocalDateTime.now()));

        when(mockCommunicator.getMeasurementsOfStation(eq("123"), any()))
                .thenReturn(List.of(
                        new Measurement(
                                Collections.singletonMap("level", 1.23),
                                Collections.singletonMap("info", "test")
                        )
                ));

        // TODO: Fix this
        //connector = new IecConnector(options);
    }

    @AfterEach
    void tearDown() {
        connector.close();
        communicatorFactoryMock.close();
    }

    @Test
    void testSendsMeasurements() throws InterruptedException {
        Thread.sleep(1000);

        verify(mockCommunicator, atLeastOnce())
                .getMeasurementsOfStation(eq("123"), any());
    }
}
