package com.stm.pegelhub.supplier.api;

import com.stm.pegelhub.shared.web.DomainToDtoConverter;

import com.stm.pegelhub.supplier.domain.StationManufacturer;
import com.stm.pegelhub.supplier.api.StationManufacturerDto;
import com.stm.pegelhub.supplier.application.StationManufacturerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.stm.pegelhub.testsupport.ExampleData.STATION_MANUFACTURER;
import static com.stm.pegelhub.testsupport.ExampleDtos.CREATE_STATION_MANUFACTURER_DTO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HttpStationManufacturerControllerTest {

    private HttpStationManufacturerController sut;

    private static final StationManufacturerService SERVICE = mock(StationManufacturerService.class);

    @BeforeEach
    void setUp() {
        sut = new HttpStationManufacturerController(SERVICE);
        reset(SERVICE);
    }

    @Test
    public void constructorShouldThrowNullPointerExceptionIfApiTokenServiceIsNull() {
        assertThrows(NullPointerException.class, () -> new HttpStationManufacturerController(null));
    }

    @Test
    void createStationManufacturer() {
        when(SERVICE.createStationManufacturer(any())).thenReturn(STATION_MANUFACTURER);
        StationManufacturerDto expected = DomainToDtoConverter.convert(STATION_MANUFACTURER);
        StationManufacturerDto actual = sut.saveStationManufacturer(CREATE_STATION_MANUFACTURER_DTO);
        assertEquals(expected, actual);
    }

    @Test
    void getStationManufacturerById() {
        UUID uuid = UUID.randomUUID();
        when(SERVICE.getStationManufacturerById(uuid)).thenReturn(STATION_MANUFACTURER);
        StationManufacturerDto expected = DomainToDtoConverter.convert(STATION_MANUFACTURER);
        StationManufacturerDto actual = sut.getStationManufacturerById(uuid);
        assertEquals(expected, actual);
    }

    @Test
    void getAllStationManufacturers() {
        List<StationManufacturer> stationManufacturers = new ArrayList<>();
        stationManufacturers.add(STATION_MANUFACTURER);
        when(SERVICE.getAllStationManufacturers()).thenReturn(stationManufacturers);
        List<StationManufacturerDto> expected = DomainToDtoConverter.convert(stationManufacturers);
        List<StationManufacturerDto> actual = sut.getAllStationManufacturers();
        assertEquals(expected, actual);
    }

    @Test
    void deleteStationManufacturer() {
        UUID uuid = UUID.randomUUID();
        sut.deleteStationManufacturer(uuid);
        verify(SERVICE, times(1)).deleteStationManufacturer(uuid);
    }
}