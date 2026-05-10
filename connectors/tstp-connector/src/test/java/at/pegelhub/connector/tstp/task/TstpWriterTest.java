package at.pegelhub.connector.tstp.task;

import at.pegelhub.connector.tstp.communication.TstpCommunicator;
import at.pegelhub.connector.tstp.service.TstpCatalogService;
import at.pegelhub.lib.PegelHubCommunicator;
import at.pegelhub.lib.model.Measurement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TstpWriterTest {
    @Mock
    private TstpCommunicator tstpCommunicator;
    @Mock
    private PegelHubCommunicator phCommunicator;
    @Mock
    private TstpCatalogService tstpCatalogService;
    @InjectMocks
    private TstpWriter tstpWriter;
    private final String durationToLookBack = "24h";
    private final String stationNumber = "station123";

    @BeforeEach
    void setUp() {
        tstpWriter = new TstpWriter(phCommunicator, tstpCommunicator, durationToLookBack, stationNumber, tstpCatalogService);
    }

    @Test
    void testRun_withValidData_sendsMeasurementsToTstp() {
        String zrid = "test_zrid";
        List<Measurement> measurements = List.of(new Measurement());

        when(phCommunicator.getMeasurementsOfStation(stationNumber, durationToLookBack)).thenReturn(measurements);
        when(tstpCatalogService.getZrid()).thenReturn(zrid);

        tstpWriter.run();

        verify(phCommunicator, times(1)).getMeasurementsOfStation(stationNumber, durationToLookBack);
        verify(tstpCatalogService, times(1)).getZrid();
        verify(tstpCommunicator, times(1)).sendMeasurements(zrid, measurements);
    }

    @Test
    void testRun_withEmptyMeasurements_doesNotSendToTstp() {
        String zrid = "test_zrid";
        List<Measurement> measurements = Collections.emptyList();

        when(phCommunicator.getMeasurementsOfStation(stationNumber, durationToLookBack)).thenReturn(measurements);
        when(tstpCatalogService.getZrid()).thenReturn(zrid);

        tstpWriter.run();

        verify(phCommunicator, times(1)).getMeasurementsOfStation(stationNumber, durationToLookBack);
        verify(tstpCatalogService, times(1)).getZrid();
        verify(tstpCommunicator, times(0)).sendMeasurements(anyString(), anyList());
    }

    @Test
    void testRun_exceptionThrown_logsError() {
        when(phCommunicator.getMeasurementsOfStation(stationNumber, durationToLookBack)).thenThrow(new RuntimeException("Test exception"));

        tstpWriter.run();

        verify(phCommunicator, times(1)).getMeasurementsOfStation(stationNumber, durationToLookBack);
        verify(tstpCatalogService, times(0)).getZrid();
        verify(tstpCommunicator, times(0)).sendMeasurements(anyString(), anyList());
    }

    @Test
    void testCancel_closesCommunicator() throws Exception {
        tstpWriter.cancel();
        verify(phCommunicator, times(1)).close();
    }
}
