package at.pegelhub.measurement.application;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.measurement.domain.WriteMeasurement;
import at.pegelhub.measurement.domain.WriteMeasurements;
import at.pegelhub.measurement.persistence.MeasurementRepository;
import at.pegelhub.measurement.persistence.MeasurementPage;
import at.pegelhub.measuringpoint.application.MeasuringPointService;
import at.pegelhub.measuringpoint.domain.MeasuringPoint;
import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.station.application.StationService;
import at.pegelhub.station.domain.Station;
import at.pegelhub.station.domain.StationId;
import at.pegelhub.stationowner.domain.StationOwnerId;
import at.pegelhub.timeseries.application.TimeSeriesService;
import at.pegelhub.timeseries.domain.MeasurementRepresentation;
import at.pegelhub.timeseries.domain.ObservedPropertyCode;
import at.pegelhub.timeseries.domain.SourceAssignment;
import at.pegelhub.timeseries.domain.TimeSeries;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static at.pegelhub.shared.metadata.MetadataStatus.ACTIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static at.pegelhub.timeseries.domain.MeasurementRepresentation.CANONICAL;
import static at.pegelhub.timeseries.domain.MeasurementRepresentation.LITRES_PER_SECOND;
import static at.pegelhub.timeseries.domain.MeasurementRepresentation.METRES_ABOVE_ADRIA;

final class MeasurementServiceImplTest {

    private static final TimeSeriesId SERIES_ID = new TimeSeriesId(
            UUID.fromString("8ce8c5b6-f093-4d46-b770-7239cdfa3d76"));
    private static final MeasuringPointId POINT_ID = new MeasuringPointId(
            UUID.fromString("7f65e3b7-97b4-4016-83a3-77f51332dc01"));
    private static final ConnectorId CONNECTOR_ID = new ConnectorId(
            UUID.fromString("0d9a3c87-b41a-4663-af0a-f6ec5e6a91cf"));
    private static final StationId STATION_ID = new StationId(UUID.randomUUID());
    private static final StationOwnerId OWNER_ID = new StationOwnerId(UUID.randomUUID());
    private static final Instant OBSERVED_AT = Instant.parse("2026-04-25T10:15:30Z");
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-17T13:00:00Z"), ZoneOffset.UTC);

    private final MeasurementRepository repository = mock(MeasurementRepository.class);
    private final MeasurementAuthorizationPolicy authorization = mock(
            MeasurementAuthorizationPolicy.class);
    private final TimeSeriesService timeSeries = mock(TimeSeriesService.class);
    private final MeasuringPointService points = mock(MeasuringPointService.class);
    private final StationService stations = mock(StationService.class);
    private final MeasurementServiceImpl service = new MeasurementServiceImpl(
            repository,
            authorization,
            CLOCK,
            timeSeries,
            points,
            stations);

    @Test
    void storesCanonicalValuesUnchanged() {
        writeTarget(SERIES_ID, "water-level", CANONICAL, null);

        service.writeMeasurements(new WriteMeasurements(
                List.of(new WriteMeasurement(SERIES_ID, OBSERVED_AT, 12.5))));

        verify(repository).storeMeasurements(argThat(measurements ->
                measurements.size() == 1 && measurements.getFirst().value() == 12.5));
    }

    @Test
    void convertsAbsoluteWaterLevelUsingCurrentPnpWithoutRounding() {
        writeTarget(
                SERIES_ID,
                "water-level",
                METRES_ABOVE_ADRIA,
                new BigDecimal("154.22"));

        service.writeMeasurements(new WriteMeasurements(
                List.of(new WriteMeasurement(SERIES_ID, OBSERVED_AT, 157.3))));

        verify(repository).storeMeasurements(argThat(measurements ->
                measurements.size() == 1 && measurements.getFirst().value() == 308.0));
    }

    @Test
    void normalizesLitresBeforeWritingAndKeepsProvenance() {
        writeTarget(SERIES_ID, "discharge", LITRES_PER_SECOND, null);
        service.writeMeasurements(new WriteMeasurements(
                List.of(new WriteMeasurement(SERIES_ID, OBSERVED_AT, 1250))));
        verify(repository).storeMeasurements(List.of(
                new Measurement(SERIES_ID, OBSERVED_AT, CLOCK.instant(), 1.25, CONNECTOR_ID)));
    }

    @Test
    void checksWriterBeforeLoadingTargetMetadata() {
        doThrow(new AccessDeniedException("denied")).when(authorization).requireWriter();
        assertThatThrownBy(() -> service.writeMeasurements(new WriteMeasurements(
                List.of(new WriteMeasurement(SERIES_ID, OBSERVED_AT, 1)))))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(timeSeries, points, stations, repository);
    }

    @Test
    void authorizesEntireBatchBeforePreparingAnyConversion() {
        var absolute = writeTarget(
                SERIES_ID, "water-level", METRES_ABOVE_ADRIA, null);
        var otherId = new TimeSeriesId(UUID.randomUUID());
        var other = writeTarget(
                otherId, "water-temperature", CANONICAL, null);
        doThrow(new AccessDeniedException("not the source connector"))
                .when(authorization).requireWrite(CONNECTOR_ID, other);

        assertThatThrownBy(() -> service.writeMeasurements(new WriteMeasurements(
                List.of(
                        new WriteMeasurement(SERIES_ID, OBSERVED_AT, 155.56),
                        new WriteMeasurement(otherId, OBSERVED_AT, 12.5)))))
                .isInstanceOf(AccessDeniedException.class);
        verify(authorization).requireWrite(CONNECTOR_ID, absolute);
        verify(authorization).requireWrite(CONNECTOR_ID, other);
        verifyNoInteractions(repository);
    }

    @Test
    void missingGaugeZeroFailsDuringPreparationAfterPermissionsPass() {
        var target = writeTarget(
                SERIES_ID, "water-level", METRES_ABOVE_ADRIA, null);

        assertThatThrownBy(() -> service.writeMeasurements(new WriteMeasurements(
                List.of(new WriteMeasurement(SERIES_ID, OBSERVED_AT, 155.56)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("gauge zero");
        verify(authorization).requireWrite(CONNECTOR_ID, target);
        verifyNoInteractions(repository);
    }

    @Test
    void conversionFailureDoesNotStoreEarlierMeasurementsInTheBatch() {
        writeTarget(SERIES_ID, "water-level", METRES_ABOVE_ADRIA, BigDecimal.ZERO);

        assertThatThrownBy(() -> service.writeMeasurements(new WriteMeasurements(List.of(
                new WriteMeasurement(SERIES_ID, OBSERVED_AT, 1),
                new WriteMeasurement(SERIES_ID, OBSERVED_AT.plusSeconds(1), Double.MAX_VALUE)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finite");

        verifyNoInteractions(repository);
    }

    @Test
    void loadsEachSeriesAndSharedHierarchyOnceAndKeepsBatchProvenance() {
        var water = writeTarget(
                SERIES_ID,
                "water-level",
                METRES_ABOVE_ADRIA,
                new BigDecimal("152.68"));
        var temperatureId = new TimeSeriesId(UUID.randomUUID());
        var temperature = writeTarget(
                temperatureId,
                "water-temperature",
                CANONICAL,
                new BigDecimal("152.68"));
        service.writeMeasurements(new WriteMeasurements(List.of(
                new WriteMeasurement(SERIES_ID, OBSERVED_AT, 155.56),
                new WriteMeasurement(temperatureId, OBSERVED_AT, 12.5),
                new WriteMeasurement(SERIES_ID, OBSERVED_AT.plusSeconds(1), 155.57))));

        verify(timeSeries).get(SERIES_ID);
        verify(timeSeries).get(temperatureId);
        verify(points).get(POINT_ID);
        verify(stations).get(STATION_ID);
        verify(authorization).requireWriter();
        verify(authorization).requireWrite(CONNECTOR_ID, water);
        verify(authorization).requireWrite(CONNECTOR_ID, temperature);
        verify(repository).storeMeasurements(List.of(
                new Measurement(SERIES_ID, OBSERVED_AT, CLOCK.instant(), 288, CONNECTOR_ID),
                new Measurement(temperatureId, OBSERVED_AT, CLOCK.instant(), 12.5, CONNECTOR_ID),
                new Measurement(
                        SERIES_ID,
                        OBSERVED_AT.plusSeconds(1),
                        CLOCK.instant(),
                        289,
                        CONNECTOR_ID)));
    }

    @Test
    void defaultReadIsCanonicalEvenWhenSourceUsesLitres() {
        when(timeSeries.get(SERIES_ID)).thenReturn(new TimeSeries(
                SERIES_ID,
                POINT_ID,
                new ObservedPropertyCode("discharge"),
                ACTIVE,
                new SourceAssignment(CONNECTOR_ID, LITRES_PER_SECOND)));
        var query = new MeasurementListQuery(
                SERIES_ID, window(), MeasurementOrder.DESC, 1);
        var row = new MeasurementReadRow(OBSERVED_AT, 1.25, CONNECTOR_ID);
        when(repository.listMeasurements(query)).thenReturn(
                new MeasurementPage(true, List.of(row)));

        var result = service.listMeasurements(query);
        assertThat(result.measurements()).containsExactly(row);
        assertThat(result.unit()).isEqualTo("m3/s");
        assertThat(result.query().representation()).isEqualTo(CANONICAL);
        assertThat(result.truncated()).isTrue();
        verifyNoInteractions(points);
    }

    @Test
    void outputRepresentationDoesNotDependOnSourceAssignment() {
        metadata("discharge", null);
        var query = new MeasurementListQuery(
                SERIES_ID,
                window(),
                MeasurementOrder.ASC,
                100,
                LITRES_PER_SECOND);
        when(repository.listMeasurements(query)).thenReturn(
                new MeasurementPage(
                        false,
                        List.of(new MeasurementReadRow(OBSERVED_AT, 1.25, CONNECTOR_ID))));

        var result = service.listMeasurements(query);
        assertThat(result.unit()).isEqualTo("l/s");
        assertThat(result.measurements()).containsExactly(
                new MeasurementReadRow(OBSERVED_AT, 1250, CONNECTOR_ID));
        verifyNoInteractions(points);
    }

    @ParameterizedTest
    @CsvSource({"water-level, cm", "water-temperature, Cel", "discharge, m3/s"})
    void canonicalReadsUseThePropertyUnitWithoutLoadingMeasuringPoints(String property, String unit) {
        metadata(property, null);
        var query = new MeasurementListQuery(SERIES_ID, window(), MeasurementOrder.ASC, 100);
        var row = new MeasurementReadRow(OBSERVED_AT, 12.5, CONNECTOR_ID);
        when(repository.listMeasurements(query)).thenReturn(new MeasurementPage(false, List.of(row)));

        var result = service.listMeasurements(query);

        assertThat(result.unit()).isEqualTo(unit);
        assertThat(result.measurements()).containsExactly(row);
        verifyNoInteractions(points);
    }

    @Test
    void absoluteReadsUseCurrentCoreGaugeZero() {
        metadata("water-level", new BigDecimal("152.68"));
        var query = new MeasurementListQuery(
                SERIES_ID,
                window(),
                MeasurementOrder.DESC,
                1,
                METRES_ABOVE_ADRIA);
        when(repository.listMeasurements(query)).thenReturn(
                new MeasurementPage(
                        false,
                        List.of(new MeasurementReadRow(OBSERVED_AT, 288, CONNECTOR_ID))));
        var result = service.listMeasurements(query);
        assertThat(result.measurements().getFirst().value()).isEqualTo(155.56);
        assertThat(result.unit()).isEqualTo("m");

        metadata("water-level", new BigDecimal("153.68"));
        var updatedResult = service.listMeasurements(query);
        assertThat(updatedResult.measurements().getFirst().value()).isEqualTo(156.56);
    }

    @Test
    void rejectsMissingGaugeZeroBeforeReadingEvenAnEmptyWindow() {
        metadata("water-level", null);
        assertThatThrownBy(() -> service.listMeasurements(new MeasurementListQuery(
                SERIES_ID, window(), MeasurementOrder.ASC, 1, METRES_ABOVE_ADRIA)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("gauge zero");
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @CsvSource({
            "water-level, LITRES_PER_SECOND",
            "water-temperature, LITRES_PER_SECOND",
            "water-temperature, METRES_ABOVE_ADRIA",
            "discharge, METRES_ABOVE_ADRIA"
    })
    void rejectsUnsupportedOutputsBeforeLoadingPointsOrMeasurements(
            String property, MeasurementRepresentation representation) {
        metadata(property, null);

        assertThatThrownBy(() -> service.listMeasurements(new MeasurementListQuery(
                SERIES_ID, window(), MeasurementOrder.ASC, 1, representation)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not supported");
        verifyNoInteractions(points, repository);
    }

    @Test
    void authorizesBeforeLoadingConversionMetadataOrMeasurements() {
        doThrow(new AccessDeniedException("denied"))
                .when(authorization).requireRead(SERIES_ID);
        assertThatThrownBy(() -> service.listMeasurements(new MeasurementListQuery(
                SERIES_ID, window(), MeasurementOrder.ASC, 1, METRES_ABOVE_ADRIA)))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(timeSeries, points, repository);
    }

    @Test
    void intervalMeansCarryPredecessorAcrossWindowsAndKeepZeroValues() {
        metadata("water-level", null);
        Instant from = Instant.parse("2026-06-17T10:00:00Z");
        var query = new MeasurementIntervalQuery(SERIES_ID, from, from.plusSeconds(3_600),
                "15m", "UTC", true, CANONICAL);
        intervalEvidence(from, query.to(),
                new MeasurementReadRow(from.minusSeconds(600), 18, CONNECTOR_ID),
                new MeasurementReadRow(from.plusSeconds(30), 19, CONNECTOR_ID),
                new MeasurementReadRow(from.plusSeconds(20), 18, CONNECTOR_ID),
                new MeasurementReadRow(from.plusSeconds(1_800), 0, CONNECTOR_ID));

        var result = service.listMeasurementIntervals(query);

        assertThat(result.unit()).isEqualTo("cm");
        assertThat(result.computedAt()).isEqualTo(CLOCK.instant());
        assertThat(result.intervals()).hasSize(4);
        assertThat(result.intervals().get(0)).satisfies(point -> {
            assertThat(point.mean()).isCloseTo(18.9666666667, org.assertj.core.data.Offset.offset(1e-9));
            assertThat(point.observationCount()).isEqualTo(2);
            assertThat(point.lastContributingObservedAt()).isEqualTo(from.plusSeconds(30));
            assertThat(point.supportStatus()).isEqualTo("full");
        });
        assertThat(result.intervals().get(1)).satisfies(point -> {
            assertThat(point.mean()).isEqualTo(19);
            assertThat(point.observationCount()).isZero();
            assertThat(point.lastContributingObservedAt()).isEqualTo(from.plusSeconds(30));
            assertThat(point.supportStatus()).isEqualTo("full");
        });
        assertThat(result.intervals().get(2).mean()).isZero();
        assertThat(result.intervals()).allSatisfy(point -> assertThat(point.windowStatus()).isEqualTo("closed"));
    }

    @Test
    void intervalMeanOfOppositeFiniteExtremesStaysFinite() {
        metadata("water-level", null);
        Instant from = Instant.parse("2026-06-17T10:00:00Z");
        var query = new MeasurementIntervalQuery(SERIES_ID, from, from.plusSeconds(900),
                "15m", "UTC", true, CANONICAL);
        intervalEvidence(from, query.to(),
                new MeasurementReadRow(from.minusSeconds(1), Double.MAX_VALUE, CONNECTOR_ID),
                new MeasurementReadRow(from.plusSeconds(450), -Double.MAX_VALUE, CONNECTOR_ID));
        assertThat(service.listMeasurementIntervals(query).intervals().getFirst().mean()).isZero();
    }

    @Test
    void intervalMeanUsesCoreAffineRepresentationConversion() {
        metadata("discharge", null);
        Instant from = Instant.parse("2026-06-17T10:00:00Z");
        var query = new MeasurementIntervalQuery(SERIES_ID, from, from.plusSeconds(900),
                "15m", "UTC", true, LITRES_PER_SECOND);
        intervalEvidence(from, query.to(),
                new MeasurementReadRow(from.minusSeconds(1), 18, CONNECTOR_ID),
                new MeasurementReadRow(from.plusSeconds(450), 19, CONNECTOR_ID));
        var result = service.listMeasurementIntervals(query);
        assertThat(result.unit()).isEqualTo("l/s");
        assertThat(result.intervals().getFirst().mean()).isEqualTo(18_500);
        assertThat(result.intervals().getFirst().observationCount()).isEqualTo(1);
    }

    @Test
    void intervalMeanUsesGaugeZeroForAbsoluteWaterLevel() {
        metadata("water-level", new BigDecimal("152.68"));
        Instant from = Instant.parse("2026-06-17T10:00:00Z");
        var query = new MeasurementIntervalQuery(SERIES_ID, from, from.plusSeconds(900),
                "15m", "UTC", true, METRES_ABOVE_ADRIA);
        intervalEvidence(from, query.to(),
                new MeasurementReadRow(from.minusSeconds(1), 288, CONNECTOR_ID));

        var result = service.listMeasurementIntervals(query);

        assertThat(result.unit()).isEqualTo("m");
        assertThat(result.intervals().getFirst().mean()).isEqualTo(155.56);
    }

    @Test
    void closedOnlyOmitsCurrentWindowWithoutReadingIt() {
        metadata("water-level", null);
        Instant from = Instant.parse("2026-06-17T12:00:00Z");
        var query = new MeasurementIntervalQuery(SERIES_ID, from, from.plusSeconds(7_200),
                "1h", "UTC", true, CANONICAL);
        var result = service.listMeasurementIntervals(query);
        assertThat(result.intervals()).hasSize(1);
        verify(repository).listIntervalEvidence(
                eq(SERIES_ID), argThat(window -> window.to().equals(CLOCK.instant())), eq(100_000));
    }

    @Test
    void openWindowIsClearlyMarkedWhenRequested() {
        metadata("water-level", null);
        Instant from = Instant.parse("2026-06-17T13:00:00Z");
        var query = new MeasurementIntervalQuery(SERIES_ID, from, from.plusSeconds(3_600),
                "1h", "UTC", false, CANONICAL);
        assertThat(service.listMeasurementIntervals(query).intervals())
                .singleElement().satisfies(point -> {
                    assertThat(point.windowStatus()).isEqualTo("open");
                    assertThat(point.supportStatus()).isEqualTo("absent");
                });
    }

    @Test
    void intervalReadAuthorizesBeforeRepresentationAndStorage() {
        doThrow(new AccessDeniedException("denied")).when(authorization).requireRead(SERIES_ID);
        var query = new MeasurementIntervalQuery(SERIES_ID,
                Instant.parse("2026-06-17T12:00:00Z"), CLOCK.instant(), "1h", "UTC", true, CANONICAL);
        assertThatThrownBy(() -> service.listMeasurementIntervals(query)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(timeSeries, points, repository);
    }

    @Test
    void intervalReadRejectsUnsupportedRepresentationBeforeStorage() {
        metadata("water-temperature", null);
        var query = new MeasurementIntervalQuery(SERIES_ID,
                Instant.parse("2026-06-17T12:00:00Z"), CLOCK.instant(),
                "1h", "UTC", true, LITRES_PER_SECOND);
        assertThatThrownBy(() -> service.listMeasurementIntervals(query))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not supported");
        verifyNoInteractions(repository);
    }

    private void metadata(String property, BigDecimal gaugeZero) {
        when(timeSeries.get(SERIES_ID)).thenReturn(new TimeSeries(
                SERIES_ID,
                POINT_ID,
                new ObservedPropertyCode(property),
                ACTIVE,
                null));
        if (property.equals("water-level")) {
            when(points.get(POINT_ID)).thenReturn(new MeasuringPoint(
                    POINT_ID,
                    new StationId(UUID.randomUUID()),
                    "Gauge",
                    ACTIVE,
                    null,
                    gaugeZero,
                    null));
        }
    }

    private void intervalEvidence(Instant from, Instant to, MeasurementReadRow... rows) {
        when(repository.listIntervalEvidence(
                eq(SERIES_ID),
                argThat(window -> window.from().equals(from) && window.to().equals(to)),
                eq(100_000)))
                .thenReturn(List.of(rows));
    }

    private static MeasurementWindow window() {
        return new MeasurementWindow(
                OBSERVED_AT.minusSeconds(1),
                OBSERVED_AT.plusSeconds(3599),
                null);
    }

    private MeasurementWriteTarget writeTarget(
            TimeSeriesId id,
            String property,
            MeasurementRepresentation representation,
            BigDecimal gaugeZero) {
        var series = new TimeSeries(
                id,
                POINT_ID,
                new ObservedPropertyCode(property),
                ACTIVE,
                new SourceAssignment(CONNECTOR_ID, representation));
        var point = new MeasuringPoint(
                POINT_ID,
                STATION_ID,
                "Gauge",
                ACTIVE,
                null,
                gaugeZero,
                null);
        var station = new Station(STATION_ID, OWNER_ID, "Station", "River", ACTIVE);
        when(authorization.requireWriter()).thenReturn(CONNECTOR_ID);
        when(timeSeries.get(id)).thenReturn(series);
        when(points.get(POINT_ID)).thenReturn(point);
        when(stations.get(STATION_ID)).thenReturn(station);
        return new MeasurementWriteTarget(series, point, station);
    }
}
