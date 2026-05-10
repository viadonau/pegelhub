package com.stm.pegelhub.taker.api;

import com.stm.pegelhub.shared.web.DomainToDtoConverter;

import com.stm.pegelhub.taker.domain.Taker;
import com.stm.pegelhub.taker.api.TakerDto;
import com.stm.pegelhub.auth.application.AuthorizationService;
import com.stm.pegelhub.taker.application.TakerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.stm.pegelhub.testsupport.ExampleData.TAKER;
import static com.stm.pegelhub.testsupport.ExampleDtos.CREATE_TAKER_DTO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HttpTakerControllerTest {

    private HttpTakerController sut;

    private static final AuthorizationService AUTHORIZATION_SERVICE = mock(AuthorizationService.class);
    private static final TakerService SERVICE = mock(TakerService.class);

    @BeforeEach
    void setUp() {
        sut = new HttpTakerController(AUTHORIZATION_SERVICE, SERVICE);
        reset(AUTHORIZATION_SERVICE);
        when(AUTHORIZATION_SERVICE.authorize(anyString())).thenReturn(UUID.randomUUID());
        reset(SERVICE);
    }

    @Test
    public void constructorShouldThrowNullPointerExceptionIfApiTokenServiceIsNull() {
        assertThrows(NullPointerException.class, () -> new HttpTakerController(null, SERVICE));
        assertThrows(NullPointerException.class, () -> new HttpTakerController(AUTHORIZATION_SERVICE, null));
    }

    @Test
    void createTaker() {
        when(SERVICE.saveTaker(any())).thenReturn(TAKER);
        TakerDto expected = DomainToDtoConverter.convert(TAKER);
        TakerDto actual = sut.saveTaker("", CREATE_TAKER_DTO);
        assertEquals(expected, actual);
        verify(AUTHORIZATION_SERVICE).authorize("");
    }

    @Test
    void getTakerById() {
        UUID uuid = UUID.randomUUID();
        when(SERVICE.getTakerById(uuid)).thenReturn(TAKER);
        TakerDto expected = DomainToDtoConverter.convert(TAKER);
        TakerDto actual = sut.getTakerById(uuid);
        assertEquals(expected, actual);
    }

    @Test
    void getAllTakers() {
        List<Taker> takers = new ArrayList<>();
        takers.add(TAKER);
        when(SERVICE.getAllTakers()).thenReturn(takers);
        List<TakerDto> expected = DomainToDtoConverter.convert(takers);
        List<TakerDto> actual = sut.getAllTakers();
        assertEquals(expected, actual);
    }

    @Test
    void deleteTaker() {
        UUID uuid = UUID.randomUUID();
        sut.deleteTaker(uuid);
        verify(SERVICE, times(1)).deleteTaker(uuid);
    }
}
