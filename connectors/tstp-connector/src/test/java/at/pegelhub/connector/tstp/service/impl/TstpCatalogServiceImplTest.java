package at.pegelhub.connector.tstp.service.impl;

import at.pegelhub.connector.tstp.communication.TstpCommunicator;
import at.pegelhub.connector.tstp.service.model.XmlQueryResponse;
import at.pegelhub.connector.tstp.service.model.XmlQueryTsAttribut;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TstpCatalogServiceImplTest {
    @Mock
    private TstpCommunicator communicator;
    @InjectMocks
    private TstpCatalogServiceImpl tstpCatalogService;
    private final int dbms = 1;

    @Test
    void testGetZrid_catalogValid_returnsZrid() {
        XmlQueryTsAttribut attr = new XmlQueryTsAttribut();
        attr.setZrid("zrid_value");
        XmlQueryResponse response = new XmlQueryResponse();
        response.setDef(List.of(attr));

        when(communicator.getCatalog(dbms)).thenReturn(response);

        tstpCatalogService = new TstpCatalogServiceImpl(communicator, dbms);

        String zrid = tstpCatalogService.getZrid();
        assertEquals("zrid_value", zrid);
        verify(communicator, times(1)).getCatalog(dbms);
    }

    @Test
    void testGetZrid_catalogIsNull_returnsEmptyString() {
        tstpCatalogService = new TstpCatalogServiceImpl(communicator, dbms);

        String zrid = tstpCatalogService.getZrid();
        assertEquals("", zrid);
    }

    @Test
    void testGetMaxFocusEnd_catalogIsValid_returnsMaxFocusEnd() {
        XmlQueryTsAttribut attr = new XmlQueryTsAttribut();
        attr.setMaxFocusEnd("2024-05-26T21:30:00Z");
        XmlQueryResponse response = new XmlQueryResponse();
        response.setDef(List.of(attr));

        when(communicator.getCatalog(dbms)).thenReturn(response);

        tstpCatalogService = new TstpCatalogServiceImpl(communicator, dbms);

        Instant maxFocusEnd = tstpCatalogService.getMaxFocusEnd();
        assertEquals(Instant.parse("2024-05-26T21:30:00Z"), maxFocusEnd);
    }

    @Test
    void testGetMaxFocusEnd_catalogIsNull_returnsNull() {
        tstpCatalogService = new TstpCatalogServiceImpl(communicator, dbms);

        Instant maxFocusEnd = tstpCatalogService.getMaxFocusEnd();
        assertNull(maxFocusEnd);
    }
}
