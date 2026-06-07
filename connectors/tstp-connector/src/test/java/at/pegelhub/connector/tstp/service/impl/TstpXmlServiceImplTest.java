package at.pegelhub.connector.tstp.service.impl;

import at.pegelhub.connector.tstp.service.model.XmlQueryResponse;
import at.pegelhub.connector.tstp.service.model.XmlTsResponse;
import at.pegelhub.lib.model.Measurement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TstpXmlServiceImplTest {
    private TstpXmlServiceImpl tstpXmlService;

    @BeforeEach
    void setUp() {
        tstpXmlService = new TstpXmlServiceImpl(new TstpBinaryServiceImpl());
    }

    @Test
    void testParseXmlGetResponseToMeasurements() {
        String sampleXmlTsData = "<TSD RELEASE=\"1\">\n" +
                "        <DEF REIHENART=\"Z\" TEXT=\"Nein\" DEFART=\"K\" EINHEIT=\"cm\" LEN=\"348\" ANZ=\"29\" />\n" +
                "        <DATA><![CDATA[\n" +
                "AAfaCAMNHgBEJizNAAfaCAMNLQBEJmAAAAfaCAMOAABEJgZmAAfaCAMODwBE\n" +
                "JizNAAfaCAMOHgBEJeAAAAfaCAMOLQBEJrmaAAfaCAMPAABEJkAAAAfaCAMP\n" +
                "DwBEJoAAAAfaCAMPHgBEJiZmAAfaCAMPLQBEJpmaAAfaCAMQAABEJwzNAAfa\n" +
                "CAMQDwBEJuZmAAfaCAMQHgBEJ5maAAfaCAMQLQBEJ8zNAAfaCAMRAABEKAZm\n" +
                "AAfaCAMRDwBEKKAAAAfaCAMRHgBEKHMzAAfaCAMRLQBEKKAAAAfaCAMSAABE\n" +
                "KIZmAAfaCAMSDwBEKJmaAAfaCAMSHgBEKKAAAAfaCAMSLQBEKHMzAAfaCAMT\n" +
                "AABEKEzNAAfaCAMTDwBEKCAAAAfaCAMTHgBEKQAAAAfaCAMTLQBEKEzNAAfa\n" +
                "CAMUAABEKBmaAAfaCAMUDwBEKCzNAAfaCAMUDwVEKC2x\n" +
                "]]></DATA>\n" +
                "</TSD>\n";
        List<Measurement> measurements = tstpXmlService.parseXmlGetResponseToMeasurements(sampleXmlTsData);
        assertEquals(29, measurements.size());
    }

    @Test
    void testParseXmlCatalog() {
        String sampleXmlCatalog = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
                "<TSQ RELEASE=\"1\">\n" +
                "        <TSATTR>\n" +
                "                <ZRID>XKNg1sGkbQnpWLTZezsPgA</ZRID>\n" +
                "                <MAXFOCUS-Start>2010-08-03T13:30:00Z</MAXFOCUS-Start>\n" +
                "                <MAXFOCUS-End>2025-10-13T09:15:05Z</MAXFOCUS-End>\n" +
                "                <MAXQUAL>0</MAXQUAL>\n" +
                "                <WRITABLE>True</WRITABLE>\n" +
                "                <PARAMETER>Wasserstand</PARAMETER>\n" +
                "                <ORT>10001373</ORT>\n" +
                "                <DEFART>K</DEFART>\n" +
                "                <AUSSAGE></AUSSAGE>\n" +
                "                <XDISTANZ></XDISTANZ>\n" +
                "                <XFAKTOR>0</XFAKTOR>\n" +
                "                <HERKUNFT>O</HERKUNFT>\n" +
                "                <REIHENART>Z</REIHENART>\n" +
                "                <VERSION>0</VERSION>\n" +
                "                <X>0</X>\n" +
                "                <Y>0</Y>\n" +
                "                <GUELTVON></GUELTVON>\n" +
                "                <GUELTBIS></GUELTBIS>\n" +
                "                <EINHEIT>cm</EINHEIT>\n" +
                "                <MESSGENAU>0.0001</MESSGENAU>\n" +
                "                <FTOLERANZ>-1.0000</FTOLERANZ>\n" +
                "                <NWGRENZE>0.1000</NWGRENZE>\n" +
                "                <SUBORT></SUBORT>\n" +
                "                <KOMMENTAR></KOMMENTAR>\n" +
                "                <HOEHE>0</HOEHE>\n" +
                "                <YTYP></YTYP>\n" +
                "                <XEINHEIT></XEINHEIT>\n" +
                "                <QUELLE></QUELLE>\n" +
                "                <PUBLIZIERT>False</PUBLIZIERT>\n" +
                "                <PARMERKMAL></PARMERKMAL>\n" +
                "                <HAUPTREIHE>True</HAUPTREIHE>\n" +
                "                <FTOLREL>False</FTOLREL>\n" +
                "                <ZWECK></ZWECK>\n" +
                "                <YNUM>0</YNUM>\n" +
                "                <MITQM>True</MITQM>\n" +
                "                <AUTOEXPORT>False</AUTOEXPORT>\n" +
                "                <MAXTEXTFOCUS-Start>0000-00-00T00:00:00Z</MAXTEXTFOCUS-Start>\n" +
                "                <MAXTEXTFOCUS-End>0000-00-00T00:00:00Z</MAXTEXTFOCUS-End>\n" +
                "                <TIMESTAMP>1700524655</TIMESTAMP>\n" +
                "        </TSATTR>\n" +
                "</TSQ>";
        XmlQueryResponse xmlQueryResponse = tstpXmlService.parseXmlCatalog(sampleXmlCatalog);
        assertEquals("XKNg1sGkbQnpWLTZezsPgA",xmlQueryResponse.getDef().get(0).getZrid());
        assertEquals("2025-10-13T09:15:05Z",xmlQueryResponse.getDef().get(0).getMaxFocusEnd());
    }

    @Test
    void testParseXmlPutResponse() {
        String xmlPutResponse = "<TSR RELEASE=\"1\">\n" +
                "confirm</TSR>\n";
        XmlTsResponse xmlTsResponse = tstpXmlService.parseXmlPutResponse(xmlPutResponse);
        assertNotNull(xmlTsResponse);
        assertEquals("\nconfirm",xmlTsResponse.getMessage());
    }

    @Test
    void testParseXmlPutRequest() {
        List<Measurement> measurements = new ArrayList<>();
        Instant dateTime = Instant.parse("2010-08-03T13:30:00Z");
        measurements.add(new Measurement(null, dateTime, 664.7));

        String xmlPutRequest = tstpXmlService.parseXmlPutRequest(measurements);
        assertTrue(xmlPutRequest.contains("ANZ=\"1\""));
    }
}
