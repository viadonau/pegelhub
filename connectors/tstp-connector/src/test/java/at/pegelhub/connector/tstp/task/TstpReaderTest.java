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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private final UUID timeSeriesId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        tstpReader = new TstpReader(phCommunicator, tstpCommunicator, durationToLookBack, timeSeriesId, tstpCatalogService);
    }

    @Test
    void testRun_withValidData_sendsMeasurementsToCore() {
        String zrid = "test_zrid";

        when(tstpCatalogService.getZrid()).thenReturn(zrid);
        when(tstpCommunicator.getMeasurements(eq(zrid), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(new Measurement(null, Instant.parse("2026-06-07T10:15:30Z"), 42.0)));

        tstpReader.run();

        verify(tstpCatalogService, times(1)).getZrid();
        verify(tstpCommunicator, times(1)).getMeasurements(eq(zrid), any(Instant.class), any(Instant.class));
        verify(phCommunicator, times(1)).sendMeasurements(argThat(measurements -> {
            Measurement sent = measurements.iterator().next();
            assertEquals(timeSeriesId, sent.getTimeSeriesId());
            assertEquals(42.0, sent.getValue());
            return true;
        }));
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
