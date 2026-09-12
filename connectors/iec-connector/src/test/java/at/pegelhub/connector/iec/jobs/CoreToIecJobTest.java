package at.pegelhub.connector.iec.jobs;

import at.pegelhub.connector.iec.datapoints.DataPointMapping;
import at.pegelhub.connector.iec.datapoints.IecMappingIndex;
import at.pegelhub.connector.iec.iec.IecClient;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.model.Measurement;
import at.pegelhub.lib.model.MeasurementRepresentation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static at.pegelhub.lib.config.MappingDirection.CORE_TO_EXTERNAL;
import static at.pegelhub.lib.config.MappingDirection.EXTERNAL_TO_CORE;
import static at.pegelhub.lib.model.MeasurementRepresentation.CANONICAL;
import static at.pegelhub.lib.model.MeasurementRepresentation.METRES_ABOVE_ADRIA;
import static org.mockito.Mockito.*;

class CoreToIecJobTest {

    private final IecClient iec = mock(IecClient.class);
    private final PegelHubClient core = mock(PegelHubClient.class);

    @Test
    void sendsOnlyOutboundMappingsWithAnAvailableMeasurement() {
        var available = UUID.randomUUID();
        var empty = UUID.randomUUID();
        var inbound = UUID.randomUUID();
        var mappings = new IecMappingIndex(List.of(
                new DataPointMapping(11, available, CORE_TO_EXTERNAL),
                new DataPointMapping(22, empty, CORE_TO_EXTERNAL),
                new DataPointMapping(33, inbound, EXTERNAL_TO_CORE)));
        var latest = measurement(available, 1.1);
        when(core.getLatestMeasurementOfTimeSeries(available, CANONICAL)).thenReturn(Optional.of(latest));
        when(core.getLatestMeasurementOfTimeSeries(empty, CANONICAL)).thenReturn(Optional.empty());

        new CoreToIecJob(iec, mappings, core).run();

        verify(iec).sendMeasurement(11, latest);
        verifyNoMoreInteractions(iec);
        verify(core).getLatestMeasurementOfTimeSeries(available, CANONICAL);
        verify(core).getLatestMeasurementOfTimeSeries(empty, CANONICAL);
        verifyNoMoreInteractions(core);
    }

    @Test
    void continuesOtherIoasAfterReadOrSendFailure() {
        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        var third = UUID.randomUUID();
        var mappings = new IecMappingIndex(List.of(
                new DataPointMapping(11, first, CORE_TO_EXTERNAL),
                new DataPointMapping(22, second, CORE_TO_EXTERNAL),
                new DataPointMapping(33, third, CORE_TO_EXTERNAL)));
        when(core.getLatestMeasurementOfTimeSeries(first, CANONICAL))
                .thenThrow(new RuntimeException("Core unavailable"));
        when(core.getLatestMeasurementOfTimeSeries(second, CANONICAL))
                .thenReturn(Optional.of(measurement(second, 2)));
        var lastMeasurement = measurement(third, 3);
        when(core.getLatestMeasurementOfTimeSeries(third, CANONICAL))
                .thenReturn(Optional.of(lastMeasurement));
        doThrow(new IllegalStateException("IEC unavailable")).when(iec).sendMeasurement(eq(22), any());

        new CoreToIecJob(iec, mappings, core).run();

        verify(iec, never()).sendMeasurement(eq(11), any());
        verify(iec).sendMeasurement(eq(22), any());
        verify(iec).sendMeasurement(33, lastMeasurement);
    }

    @ParameterizedTest
    @EnumSource(value = MeasurementRepresentation.class, names = {"METRES_ABOVE_ADRIA", "LITRES_PER_SECOND"})
    void forwardsTheRequestedCoreRepresentationWithoutLocalConversion(MeasurementRepresentation representation) {
        UUID timeSeriesId = UUID.randomUUID();
        var mappings = new IecMappingIndex(List.of(
                new DataPointMapping(66059, timeSeriesId, CORE_TO_EXTERNAL, representation)));
        var converted = measurement(timeSeriesId, 155.56);
        when(core.getLatestMeasurementOfTimeSeries(timeSeriesId, representation))
                .thenReturn(Optional.of(converted));

        new CoreToIecJob(iec, mappings, core).run();

        verify(iec).sendMeasurement(66059, converted);
        verify(core).getLatestMeasurementOfTimeSeries(timeSeriesId, representation);
        verifyNoMoreInteractions(core);
    }

    @Test
    void representationFailureNeverFallsBackToSendingCanonicalValues() {
        UUID id = UUID.randomUUID();
        var mappings = new IecMappingIndex(List.of(
                new DataPointMapping(123, id, CORE_TO_EXTERNAL, METRES_ABOVE_ADRIA)));
        when(core.getLatestMeasurementOfTimeSeries(id, METRES_ABOVE_ADRIA))
                .thenThrow(new IllegalStateException("Core did not confirm representation"));

        new CoreToIecJob(iec, mappings, core).run();

        verifyNoInteractions(iec);
        verify(core).getLatestMeasurementOfTimeSeries(id, METRES_ABOVE_ADRIA);
        verifyNoMoreInteractions(core);
    }

    private static Measurement measurement(UUID timeSeriesId, double value) {
        return new Measurement(timeSeriesId, Instant.parse("2026-09-01T12:00:00Z"), value);
    }
}
