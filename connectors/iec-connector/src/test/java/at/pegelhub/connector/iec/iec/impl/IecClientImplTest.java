package at.pegelhub.connector.iec.iec.impl;

import at.pegelhub.lib.model.Measurement;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.connector.iec.datapoints.IecMappingIndex;
import at.pegelhub.connector.iec.jobs.IecToCoreJob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.openmuc.j60870.ASdu;
import org.openmuc.j60870.ASduType;
import org.openmuc.j60870.CauseOfTransmission;
import org.openmuc.j60870.Connection;
import org.openmuc.j60870.ie.IeQuality;
import org.openmuc.j60870.ie.IeShortFloat;
import org.openmuc.j60870.ie.InformationElement;
import org.openmuc.j60870.ie.InformationObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class IecClientImplTest {

    @ParameterizedTest
    @ValueSource(floats = {Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY})
    void shouldDiscardNonFiniteReadingsWithoutBlockingValidReadings(float invalid) throws Exception {
        var client = new IecClientImpl(
                "127.0.0.1", 2404, 514, Set.of(100, 200));
        Method enqueue = IecClientImpl.class.getDeclaredMethod("enqueueMeasurements", ASdu.class);
        enqueue.setAccessible(true);

        enqueue.invoke(client, shortFloat(514, 100, 1.0f));
        enqueue.invoke(client, shortFloat(514, 100, invalid));
        enqueue.invoke(client, shortFloat(514, 100, 2.0f));
        enqueue.invoke(client, shortFloat(514, 200, 3.0f));

        var grouped = client.drainGroupedMeasurements();
        assertThat(grouped.get(100)).extracting(Measurement::getValue).containsExactly(1.0, 2.0);
        assertThat(grouped.get(200)).extracting(Measurement::getValue).containsExactly(3.0);
    }

    @Test
    void shouldBuildShortFloatAsduWithExpectedFields() throws Exception {
        // Given
        var client = new IecClientImpl(
                "127.0.0.1", 2404, 514, Set.of(1));

        Connection conn = mock(Connection.class);
        Field f = IecClientImpl.class.getDeclaredField("connection");
        f.setAccessible(true);
        f.set(client, conn);

        var m = new Measurement(UUID.fromString("395c0232-d110-40fd-bd7f-2bb4a0f2009d"), Instant.now(), 12.34);

        // When
        client.sendMeasurement(66049, m);

        // Then
        var cap = ArgumentCaptor.forClass(ASdu.class);
        verify(conn).send(cap.capture());

        ASdu sent = cap.getValue();
        assertThat(sent.getTypeIdentification()).isEqualTo(ASduType.M_ME_NC_1);
        assertThat(sent.getCauseOfTransmission()).isEqualTo(CauseOfTransmission.SPONTANEOUS);
        assertThat(sent.getCommonAddress()).isEqualTo(514);
        assertThat(sent.getInformationObjects()).hasSize(1);
        assertThat(sent.getInformationObjects()[0].getInformationObjectAddress()).isEqualTo(66049);
    }

    @Test
    void shouldEnqueueOnlyRegisteredIoasAndGroup() throws Exception {
        // Given
        var client = new IecClientImpl(
                "127.0.0.1", 2404, 514, Set.of(100, 200));

        Method enqueue = IecClientImpl.class.getDeclaredMethod("enqueueMeasurements", ASdu.class);
        enqueue.setAccessible(true);

        // When: enqueue supported + unsupported IOAs
        enqueue.invoke(client, shortFloat(514, 100, 1.0f));
        enqueue.invoke(client, shortFloat(514, 300, 2.0f)); // unregistered -> ignored
        enqueue.invoke(client, shortFloat(514, 100, 3.0f));
        enqueue.invoke(client, shortFloat(514, 200, 7.0f));

        // Then
        Map<Integer, List<Measurement>> grouped = client.drainGroupedMeasurements();
        assertThat(grouped.keySet()).containsExactlyInAnyOrder(100, 200);
        assertThat(grouped.get(100)).hasSize(2);
        assertThat(grouped.get(200)).hasSize(1);

        assertThat(client.drainGroupedMeasurements()).isEmpty();
    }

    @Test
    void shouldNotReconnectInsideConnectionCallback() throws Exception {
        // Given
        var spyClient = spy(new IecClientImpl(
                "127.0.0.1", 2404, 514, Set.of()));
        doNothing().when(spyClient).connect();

        Method factory = IecClientImpl.class.getDeclaredMethod("createIecListener");
        factory.setAccessible(true);
        Object listener = factory.invoke(spyClient);

        // When
        var m = listener.getClass().getMethod("connectionClosed", java.io.IOException.class);
        m.invoke(listener, new java.io.IOException("boom"));

        // Then
        verify(spyClient, never()).connect();
    }

    @Test
    void shouldDeliverGoodReadingsAfterNaNAndTemporaryCoreFailure() throws Exception {
        var client = new IecClientImpl("127.0.0.1", 2404, 514, Set.of(100));
        var mappings = mock(IecMappingIndex.class);
        UUID timeSeriesId = UUID.randomUUID();
        when(mappings.getTimeSeriesId(100)).thenReturn(Optional.of(timeSeriesId));
        var core = mock(PegelHubClient.class);
        doThrow(new RuntimeException("Core unavailable")).doNothing().when(core).sendMeasurements(anyList());
        var job = new IecToCoreJob(client, mappings, core);
        Method enqueue = IecClientImpl.class.getDeclaredMethod("enqueueMeasurements", ASdu.class);
        enqueue.setAccessible(true);

        enqueue.invoke(client, shortFloat(514, 100, 1.0f));
        enqueue.invoke(client, shortFloat(514, 100, Float.NaN));
        job.run();
        enqueue.invoke(client, shortFloat(514, 100, 2.0f));
        job.run();
        enqueue.invoke(client, shortFloat(514, 100, 3.0f));
        job.run();

        ArgumentCaptor<List<Measurement>> sent = ArgumentCaptor.captor();
        verify(core, times(3)).sendMeasurements(sent.capture());
        assertThat(sent.getAllValues().get(0)).extracting(Measurement::getValue).containsExactly(1.0);
        assertThat(sent.getAllValues().get(1)).extracting(Measurement::getValue).containsExactly(1.0, 2.0);
        assertThat(sent.getAllValues().get(2)).extracting(Measurement::getValue).containsExactly(3.0);
        assertThat(sent.getAllValues()).flatExtracting(batch -> batch)
                .allSatisfy(measurement -> assertThat(measurement.getTimeSeriesId()).isEqualTo(timeSeriesId));
    }


    // Helpers
    private ASdu shortFloat(int commonAddress, int ioa, float value) {
        InformationElement[] elems = new InformationElement[]{
                new IeShortFloat(value),
                new IeQuality(false, false, false, false, false)
        };
        InformationObject io = new InformationObject(ioa, elems);
        return new ASdu(
                ASduType.M_ME_NC_1,
                false,
                CauseOfTransmission.SPONTANEOUS,
                false,
                false,
                0,
                commonAddress,
                io
        );
    }
}
