package at.pegelhub.supplier.application;

import at.pegelhub.supplier.domain.StationManufacturer;
import at.pegelhub.supplier.persistence.StationManufacturerRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static at.pegelhub.testsupport.ExampleData.STATION_MANUFACTURER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

final class StationManufacturerServiceImplTest {

    private StationManufacturerServiceImpl stationManufacturerService;
    private static final StationManufacturerRepository REPOSITORY = mock(StationManufacturerRepository.class);

    @BeforeEach
    public void prepare() {
        stationManufacturerService = new StationManufacturerServiceImpl(REPOSITORY);
        reset(REPOSITORY);
    }

    @Test
    public void constructorWithNullArgsThrowsNPE() {
        assertThrows(NullPointerException.class, () -> new StationManufacturerServiceImpl(null));
    }

    @Test
    public void createStationManufacturer() {
        when(REPOSITORY.saveStationManufacturer(any())).thenReturn(STATION_MANUFACTURER);

        StationManufacturer result = stationManufacturerService.createStationManufacturer(STATION_MANUFACTURER);
        assertEquals(STATION_MANUFACTURER, result);
        verify(REPOSITORY, times(1)).saveStationManufacturer(any());
    }

    @Test
    public void getById() {
        when(REPOSITORY.getById(any())).thenReturn(STATION_MANUFACTURER);

        StationManufacturer result = stationManufacturerService.getStationManufacturerById(UUID.randomUUID());
        assertEquals(STATION_MANUFACTURER, result);
        verify(REPOSITORY, times(1)).getById(any());
    }


    @Test
    public void getAll() {
        when(REPOSITORY.getAllStationManufacturers()).thenReturn(List.of(STATION_MANUFACTURER));

        List<StationManufacturer> result = stationManufacturerService.getAllStationManufacturers();
        assertEquals(1, result.size());
        Assertions.assertThat(result).containsOnly(STATION_MANUFACTURER);
        verify(REPOSITORY, times(1)).getAllStationManufacturers();
    }
}