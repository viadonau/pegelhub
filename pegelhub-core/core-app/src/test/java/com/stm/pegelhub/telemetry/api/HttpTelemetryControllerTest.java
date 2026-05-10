package com.stm.pegelhub.telemetry.api;

import com.stm.pegelhub.telemetry.domain.Telemetry;
import com.stm.pegelhub.telemetry.application.TelemetryService;
import com.stm.pegelhub.auth.application.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static com.stm.pegelhub.testsupport.ExampleData.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

public class HttpTelemetryControllerTest {

    private static final String API_KEY = "api_key";
    private HttpTelemetryController sut;

    private static final AuthorizationService AUTHORIZATION_SERVICE = mock(AuthorizationService.class);
    private static final TelemetryService SERVICE = mock(TelemetryService.class);

    @BeforeEach
    public void setUp() {
        sut = new HttpTelemetryController(AUTHORIZATION_SERVICE, SERVICE);
        reset(AUTHORIZATION_SERVICE);
        when(AUTHORIZATION_SERVICE.authorize(anyString())).thenReturn(UUID.randomUUID());
        reset(SERVICE);
    }

    @Test
    public void constructorShouldThrowNullPointerExceptionIfTelemetryServiceIsNull() {
        assertThrows(NullPointerException.class, () -> new HttpTelemetryController(null, SERVICE));
        assertThrows(NullPointerException.class, () -> new HttpTelemetryController(AUTHORIZATION_SERVICE, null));
    }

    @Test
    public void writeTelemetryDataShouldSaveTelemetry() {
        when(SERVICE.saveTelemetry(TELEMETRY)).thenReturn(TELEMETRY);

        Telemetry result = sut.writeTelemetryData(API_KEY, TELEMETRY);

        verify(AUTHORIZATION_SERVICE, times(1)).authorize(API_KEY);
        verify(SERVICE, times(1)).saveTelemetry(TELEMETRY);
        assertSame(TELEMETRY, result);
    }

    @Test
    public void findTelemetryInRangeShouldReturnTelemetryInRange() {
        String range = "last_hour";
        when(SERVICE.getByRange(range)).thenReturn(TELEMETRIES);

        List<Telemetry> result = sut.findTelemetryInRange(API_KEY, range);

        verify(AUTHORIZATION_SERVICE, times(1)).authorize(API_KEY);
        verify(SERVICE, times(1)).getByRange(range);
        assertSame(TELEMETRIES, result);
    }

    @Test
    public void findTelemetryByIdShouldReturnLastData() {
        when(SERVICE.getLastData(ID)).thenReturn(TELEMETRY);

        Telemetry result = sut.findTelemetryById(API_KEY, ID);

        verify(AUTHORIZATION_SERVICE, times(1)).authorize(API_KEY);
        verify(SERVICE, times(1)).getLastData(ID);
        assertSame(TELEMETRY, result);
    }
}
