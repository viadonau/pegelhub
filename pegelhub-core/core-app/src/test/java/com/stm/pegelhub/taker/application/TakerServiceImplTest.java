package com.stm.pegelhub.taker.application;

import com.stm.pegelhub.taker.domain.Taker;
import com.stm.pegelhub.connector.persistence.ConnectorRepository;
import com.stm.pegelhub.contact.persistence.ContactRepository;
import com.stm.pegelhub.taker.persistence.TakerRepository;
import com.stm.pegelhub.taker.persistence.TakerServiceManufacturerRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static com.stm.pegelhub.testsupport.ExampleData.TAKER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

final class TakerServiceImplTest {

    private TakerServiceImpl takerService;
    private static final TakerRepository REPOSITORY = mock(TakerRepository.class);
    private static final TakerServiceManufacturerRepository TAKER_SERVICE_MANUFACTURER_REPOSITORY =
            mock(TakerServiceManufacturerRepository.class);
    private static final ConnectorRepository CONNECTOR_REPOSITORY = mock(ConnectorRepository.class);
    private static final ContactRepository CONTACT_REPOSITORY = mock(ContactRepository.class);

    @BeforeEach
    public void prepare() {
        takerService = new TakerServiceImpl(REPOSITORY, TAKER_SERVICE_MANUFACTURER_REPOSITORY, CONNECTOR_REPOSITORY,
                CONTACT_REPOSITORY);
        reset(REPOSITORY, TAKER_SERVICE_MANUFACTURER_REPOSITORY, CONNECTOR_REPOSITORY, CONTACT_REPOSITORY);
        when(TAKER_SERVICE_MANUFACTURER_REPOSITORY.saveTakerServiceManufacturer(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(CONNECTOR_REPOSITORY.saveConnector(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(CONTACT_REPOSITORY.saveContact(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    public void constructorWithNullArgsThrowsNPE() {
        assertThrows(NullPointerException.class, () -> new TakerServiceImpl(null, TAKER_SERVICE_MANUFACTURER_REPOSITORY, CONNECTOR_REPOSITORY, CONTACT_REPOSITORY));
        assertThrows(NullPointerException.class, () -> new TakerServiceImpl(REPOSITORY, null, CONNECTOR_REPOSITORY, CONTACT_REPOSITORY));
        assertThrows(NullPointerException.class, () -> new TakerServiceImpl(REPOSITORY, TAKER_SERVICE_MANUFACTURER_REPOSITORY, null, CONTACT_REPOSITORY));
        assertThrows(NullPointerException.class, () -> new TakerServiceImpl(REPOSITORY, TAKER_SERVICE_MANUFACTURER_REPOSITORY, CONNECTOR_REPOSITORY, null));
    }

    @Test
    public void createTaker() {
        when(REPOSITORY.saveTaker(any())).thenReturn(TAKER);

        Taker result = takerService.saveTaker(TAKER);
        assertEquals(TAKER, result);
        verify(REPOSITORY, times(1)).saveTaker(any());
    }

    @Test
    public void getById() {
        when(REPOSITORY.getById(any())).thenReturn(TAKER);

        Taker result = takerService.getTakerById(UUID.randomUUID());
        assertEquals(TAKER, result);
        verify(REPOSITORY, times(1)).getById(any());
    }


    @Test
    public void getAll() {
        when(REPOSITORY.getAllTakers()).thenReturn(List.of(TAKER));

        List<Taker> result = takerService.getAllTakers();
        assertEquals(1, result.size());
        Assertions.assertThat(result).containsOnly(TAKER);
        verify(REPOSITORY, times(1)).getAllTakers();
    }
}
