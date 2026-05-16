package at.pegelhub.taker.application;

import at.pegelhub.taker.domain.TakerServiceManufacturer;
import at.pegelhub.taker.persistence.TakerServiceManufacturerRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static at.pegelhub.testsupport.ExampleData.TAKER_SERVICE_MANUFACTURER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

final class TakerServiceManufacturerServiceImplTest {

    private TakerServiceManufacturerServiceImpl takerServiceManufacturerService;
    private static final TakerServiceManufacturerRepository REPOSITORY = mock(TakerServiceManufacturerRepository.class);

    @BeforeEach
    public void prepare() {
        takerServiceManufacturerService = new TakerServiceManufacturerServiceImpl(REPOSITORY);
        reset(REPOSITORY);
    }

    @Test
    public void constructorWithNullArgsThrowsNPE() {
        assertThrows(NullPointerException.class, () -> new TakerServiceManufacturerServiceImpl(null));
    }

    @Test
    public void createTakerServiceManufacturer() {
        when(REPOSITORY.saveTakerServiceManufacturer(any())).thenReturn(TAKER_SERVICE_MANUFACTURER);

        TakerServiceManufacturer result = takerServiceManufacturerService.createTakerServiceManufacturer(TAKER_SERVICE_MANUFACTURER);
        assertEquals(TAKER_SERVICE_MANUFACTURER, result);
        verify(REPOSITORY, times(1)).saveTakerServiceManufacturer(any());
    }

    @Test
    public void getById() {
        when(REPOSITORY.getById(any())).thenReturn(TAKER_SERVICE_MANUFACTURER);

        TakerServiceManufacturer result = takerServiceManufacturerService.getTakerServiceManufacturerById(UUID.randomUUID());
        assertEquals(TAKER_SERVICE_MANUFACTURER, result);
        verify(REPOSITORY, times(1)).getById(any());
    }


    @Test
    public void getAll() {
        when(REPOSITORY.getAllTakerServiceManufacturers()).thenReturn(List.of(TAKER_SERVICE_MANUFACTURER));

        List<TakerServiceManufacturer> result = takerServiceManufacturerService.getAllTakerServiceManufacturers();
        assertEquals(1, result.size());
        Assertions.assertThat(result).containsOnly(TAKER_SERVICE_MANUFACTURER);
        verify(REPOSITORY, times(1)).getAllTakerServiceManufacturers();
    }
}