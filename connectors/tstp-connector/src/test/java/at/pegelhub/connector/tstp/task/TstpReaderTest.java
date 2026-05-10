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

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TstpReaderTest {
    @Mock
    private TstpCommunicator tstpCommunicator;
    @Mock
    private PegelHubCommunicator phCommunicator;
    @Mock
    private TstpCatalogService tstpCatalogService;
    @InjectMocks
    private TstpReader tstpReader;
    private final Duration durationToLookBack = Duration.ofHours(24);

    @BeforeEach
    void setUp() {
        tstpReader = new TstpReader(phCommunicator, tstpCommunicator, durationToLookBack, tstpCatalogService);
    }

    @Test
    void testRun_withValidData_sendsMeasurementsToCore() {
        String zrid = "test_zrid";

        when(tstpCatalogService.getZrid()).thenReturn(zrid);
        when(tstpCommunicator.getMeasurements(eq(zrid), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(new Measurement()));

        tstpReader.run();

        verify(tstpCatalogService, times(1)).getZrid();
        verify(tstpCommunicator, times(1)).getMeasurements(eq(zrid), any(Instant.class), any(Instant.class));
        verify(phCommunicator, times(1)).sendMeasurements(anyList());
    }

    @Test
    void testRun_withEmptyMeasurements_doesNotSendToCore() throws Exception {
        String zrid = "test_zrid";

        when(tstpCatalogService.getZrid()).thenReturn(zrid);
        when(tstpCommunicator.getMeasurements(eq(zrid), any(Instant.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        tstpReader.run();

        verify(tstpCatalogService, times(1)).getZrid();
        verify(tstpCommunicator, times(1)).getMeasurements(eq(zrid), any(Instant.class), any(Instant.class));
        verify(phCommunicator, times(0)).sendMeasurements(anyList());
    }


    @Test
    void testCancel_closesCommunicator() throws Exception {
        tstpReader.cancel();
        verify(phCommunicator, times(1)).close();
    }
}
