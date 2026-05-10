package com.stm.pegelhub.measurement.api;

import com.stm.pegelhub.measurement.domain.Measurement;
import com.stm.pegelhub.measurement.api.WriteMeasurementDto;
import com.stm.pegelhub.measurement.api.WriteMeasurementsDto;
import com.stm.pegelhub.measurement.application.MeasurementService;
import com.stm.pegelhub.auth.application.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.stm.pegelhub.testsupport.ExampleData.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HttpMeasurementControllerTest {


    private HttpMeasurementController controller;
    private static final AuthorizationService AUTHORIZATION_SERVICE = mock(AuthorizationService.class);
    private static final MeasurementService SERVICE = mock(MeasurementService.class);

    @BeforeEach
    public void prepare() {
        controller = new HttpMeasurementController(AUTHORIZATION_SERVICE, SERVICE);
        reset(AUTHORIZATION_SERVICE);
        when(AUTHORIZATION_SERVICE.authorize(anyString())).thenReturn(UUID.randomUUID());
        reset(SERVICE);
    }

    @Test
    void constructor_WhenAnyArgsAreNull_ThrowsNPE() {
        assertThrows(NullPointerException.class, () -> new HttpMeasurementController(null, SERVICE));
        assertThrows(NullPointerException.class, () -> new HttpMeasurementController(AUTHORIZATION_SERVICE, null));
    }

    @Test
    void writeMeasurementData() {
        assertDoesNotThrow(() -> controller.writeMeasurementData("", new WriteMeasurementsDto(List.of(new WriteMeasurementDto(LocalDateTime.now(), Map.of("", 1.0), Map.of())))));
        verify(AUTHORIZATION_SERVICE).authorize("");
    }

    @Test
    void findMeasurementInRange() {
        when(SERVICE.getByRange("range")).thenReturn(MEASUREMENTS);
        List<Measurement> res = controller.findMeasurementInRange("", "range");
        assertEquals(MEASUREMENTS, res);
        verify(AUTHORIZATION_SERVICE).authorize("");
    }

    @Test
    void findMeasurementForSupplierInRange() {
        when(SERVICE.getBySupplierAndRange("supplier","range")).thenReturn(MEASUREMENTS);
        List<Measurement> res = controller.findMeasurementForSupplierInRange("", "supplier", "range");
        assertEquals(MEASUREMENTS, res);
        verify(AUTHORIZATION_SERVICE).authorize("");
    }

    @Test
    void findMeasurementById() {
        when(SERVICE.getLastData(ID)).thenReturn(MEASUREMENT);
        Measurement res = controller.findMeasurementById("", ID);
        assertEquals(MEASUREMENT, res);
        verify(AUTHORIZATION_SERVICE).authorize("");
    }
}
