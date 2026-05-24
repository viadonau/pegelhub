package at.pegelhub.measurement.application;

import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.domain.ConnectorStatus;
import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.measurement.domain.WriteMeasurement;
import at.pegelhub.measurement.domain.WriteMeasurements;
import at.pegelhub.measurement.persistence.MeasurementRepository;
import at.pegelhub.security.CurrentActor;
import at.pegelhub.security.PegelHubActor;
import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.supplier.domain.Supplier;
import at.pegelhub.supplier.persistence.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static at.pegelhub.testsupport.ExampleData.MEASUREMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class MeasurementServiceImplTest {

    private MeasurementServiceImpl measurementService;

    private static final SupplierRepository SUPPLIER_REPOSITORY = mock(SupplierRepository.class);
    private static final MeasurementRepository MEASUREMENT_REPOSITORY = mock(MeasurementRepository.class);
    private static final CurrentActor CURRENT_ACTOR = mock(CurrentActor.class);

    @BeforeEach
    void prepare() {
        measurementService = new MeasurementServiceImpl(SUPPLIER_REPOSITORY, MEASUREMENT_REPOSITORY, CURRENT_ACTOR);
        reset(SUPPLIER_REPOSITORY, MEASUREMENT_REPOSITORY, CURRENT_ACTOR);
        when(CURRENT_ACTOR.get()).thenReturn(new PegelHubActor("subject", "local-connector-example", Set.of()));
    }

    @Test
    void constructorWithNullArgsThrowsNpe() {
        assertThrows(NullPointerException.class, () -> new MeasurementServiceImpl(SUPPLIER_REPOSITORY, null, CURRENT_ACTOR));
        assertThrows(NullPointerException.class, () -> new MeasurementServiceImpl(null, MEASUREMENT_REPOSITORY, CURRENT_ACTOR));
        assertThrows(NullPointerException.class, () -> new MeasurementServiceImpl(SUPPLIER_REPOSITORY, MEASUREMENT_REPOSITORY, null));
    }

    @Test
    void writeMeasurementsStoresMappedMeasurements() {
        when(SUPPLIER_REPOSITORY.findByConnectorKeycloakClientId("local-connector-example"))
                .thenReturn(Optional.of(supplierWithConnector(ConnectorStatus.ACTIVE)));

        measurementService.writeMeasurements(new WriteMeasurements(List.of(new WriteMeasurement(
                LocalDateTime.now(),
                Map.of("waterLevel", 1.0),
                Map.of()))));

        verify(MEASUREMENT_REPOSITORY).storeMeasurements(any());
    }

    @Test
    void writeMeasurementsThrowsNotFoundWhenConnectorIsUnknown() {
        when(SUPPLIER_REPOSITORY.findByConnectorKeycloakClientId("local-connector-example")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> measurementService.writeMeasurements(new WriteMeasurements(List.of(
                new WriteMeasurement(LocalDateTime.now(), Map.of("waterLevel", 1.0), Map.of())
        ))));
    }

    @Test
    void writeMeasurementsThrowsAccessDeniedWhenConnectorIsSuspended() {
        when(SUPPLIER_REPOSITORY.findByConnectorKeycloakClientId("local-connector-example"))
                .thenReturn(Optional.of(supplierWithConnector(ConnectorStatus.SUSPENDED)));

        assertThrows(AccessDeniedException.class, () -> measurementService.writeMeasurements(new WriteMeasurements(List.of(
                new WriteMeasurement(LocalDateTime.now(), Map.of("waterLevel", 1.0), Map.of())
        ))));
    }

    @Test
    void getByRangeDelegatesToRepository() {
        when(MEASUREMENT_REPOSITORY.getByRange("72d")).thenReturn(List.of(MEASUREMENT));

        List<Measurement> result = measurementService.getByRange("72d");

        assertEquals(List.of(MEASUREMENT), result);
    }

    @Test
    void getBySupplierAndRangeReturnsMeasurementsForExistingSupplier() {
        when(SUPPLIER_REPOSITORY.findByStationNumber("sup")).thenReturn(Optional.of(new Supplier().withId(UUID.randomUUID())));
        when(MEASUREMENT_REPOSITORY.getByIDAndRange(any(), any())).thenReturn(List.of(MEASUREMENT));

        List<Measurement> result = measurementService.getBySupplierAndRange("sup", "72d");

        assertEquals(1, result.size());
        assertEquals(MEASUREMENT, result.getFirst());
    }

    @Test
    void getBySupplierAndRangeThrowsNotFoundForUnknownSupplier() {
        when(SUPPLIER_REPOSITORY.findByStationNumber("missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> measurementService.getBySupplierAndRange("missing", "72d"));
    }

    @Test
    void getLatestBySupplierThrowsNotFoundForUnknownSupplier() {
        when(SUPPLIER_REPOSITORY.findByStationNumber("missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> measurementService.getLatestBySupplier("missing"));
    }

    @Test
    void getAverageBySupplierAndRangeReturnsAverageMeasurement() {
        UUID supplierId = UUID.randomUUID();
        Supplier supplier = new Supplier().withId(supplierId);
        when(SUPPLIER_REPOSITORY.findByStationNumber("sup")).thenReturn(Optional.of(supplier));
        when(MEASUREMENT_REPOSITORY.getAverageByIdAndRange(supplierId, "72d")).thenReturn(MEASUREMENT);

        Measurement result = measurementService.getAverageBySupplierAndRange("sup", "72d");

        assertEquals(MEASUREMENT, result);
    }

    @Test
    void getAverageBySupplierAndRangeThrowsNotFoundForUnknownSupplier() {
        when(SUPPLIER_REPOSITORY.findByStationNumber("missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> measurementService.getAverageBySupplierAndRange("missing", "72d"));
    }

    @Test
    void getLastDataDelegatesToRepository() {
        UUID id = UUID.randomUUID();
        when(MEASUREMENT_REPOSITORY.getLastData(id)).thenReturn(MEASUREMENT);

        Measurement result = measurementService.getLastData(id);

        assertEquals(MEASUREMENT, result);
    }

    @Test
    void getSystemTimeDelegatesToRepository() {
        Timestamp ts = Timestamp.valueOf(LocalDateTime.of(2026, 1, 2, 3, 4, 5));
        when(MEASUREMENT_REPOSITORY.getSystemTime()).thenReturn(ts);

        Timestamp result = measurementService.getSystemTime();

        assertEquals(ts, result);
    }

    private static Supplier supplierWithConnector(ConnectorStatus status) {
        Supplier supplier = new Supplier().withId(UUID.randomUUID());
        supplier.setConnector(new Connector().withExternalAuth("local-connector-example", status));
        return supplier;
    }
}
