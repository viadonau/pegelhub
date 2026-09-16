package at.pegelhub.connector.tstp;

import at.pegelhub.connector.tstp.catalog.TstpCatalogResolver;
import at.pegelhub.connector.tstp.client.HttpTstpClient;
import at.pegelhub.connector.tstp.codec.TstpBinaryCodec;
import at.pegelhub.connector.tstp.config.TstpServer;
import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.config.MappingDirection;
import at.pegelhub.lib.model.Measurement;
import at.pegelhub.lib.model.MeasurementRepresentation;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TstpParameterTransferTest {
    private static final UUID SERIES = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant OBSERVED_AT = Instant.parse("2026-06-07T10:15:30Z");
    private static final Clock CLOCK = Clock.fixed(OBSERVED_AT.plusSeconds(30), ZoneOffset.UTC);

    private final Map<String, String> catalogs = new HashMap<>();
    private final Map<String, byte[]> readings = new HashMap<>();
    private final Map<String, byte[]> writes = new HashMap<>();
    private final List<String> requests = new ArrayList<>();
    private final PegelHubClient core = mock(PegelHubClient.class);
    private HttpServer server;
    private HttpTstpClient client;

    @BeforeEach
    void startTstpServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            requests.add(query);
            Map<String, String> arguments = new HashMap<>();
            for (String part : query.split("&")) {
                String[] pair = part.split("=", 2);
                arguments.put(pair[0], pair[1]);
            }
            byte[] response;
            if (arguments.get("Cmd").equals("Query")) {
                response = catalogs.get(arguments.get("Parameter")).getBytes(StandardCharsets.ISO_8859_1);
            } else if (arguments.get("Cmd").equals("Get")) {
                response = readings.get(arguments.get("ZRID"));
            } else {
                writes.put(arguments.get("ZRID"), exchange.getRequestBody().readAllBytes());
                response = "<TSR RELEASE=\"1\">confirm</TSR>".getBytes(StandardCharsets.US_ASCII);
            }
            // Deliberately no charset header: the XML declaration must govern decoding.
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        client = HttpTstpClient.open(new TstpServer("127.0.0.1", server.getAddress().getPort(), "Z"));
    }

    @AfterEach
    void stopTstpServer() {
        client.close();
        server.stop(0);
    }

    @ParameterizedTest
    @CsvSource({
            "Wasserstand,cm,CANONICAL,42.125",
            "WTemperatur,\u00b0C,CANONICAL,17.375",
            "Abfluss,l/s,LITRES_PER_SECOND,1250",
            "Abfluss,m3/s,CANONICAL,0.001953125",
            "Abfluss,m^3/s,CANONICAL,0.001953125",
            "Abfluss,m\u00b3/s,CANONICAL,0.001953125"
    })
    void transfersEachQuantityInBothDirectionsWithoutLocalConversion(
            String parameter, String unit, MeasurementRepresentation representation, double value) throws Exception {
        registerSeries(parameter, unit, value);
        TstpParameter selected = TstpParameter.from(parameter);
        TstpCatalogResolver resolver = new TstpCatalogResolver(client);
        when(core.getMeasurementsOfTimeSeries(eq(SERIES), any(), any(), eq(representation)))
                .thenReturn(List.of(new Measurement(SERIES, OBSERVED_AT, value)));

        synchronize(new TstpMapping(SERIES, 77, MappingDirection.EXTERNAL_TO_CORE, selected, unit), resolver);
        ArgumentCaptor<List<Measurement>> batch = ArgumentCaptor.captor();
        verify(core).sendMeasurements(batch.capture());
        assertEquals(1, batch.getValue().size());
        Measurement imported = batch.getValue().getFirst();
        assertEquals(SERIES, imported.getTimeSeriesId());
        assertEquals(OBSERVED_AT, imported.getObservedAt());
        assertEquals(value, imported.getValue());

        synchronize(new TstpMapping(SERIES, 77, MappingDirection.CORE_TO_EXTERNAL, selected, unit), resolver);
        verify(core).getMeasurementsOfTimeSeries(eq(SERIES), any(), any(), eq(representation));
        assertEquals(1, requests.stream().filter(q -> q.startsWith("Cmd=Query&")).count());
        assertEquals("Cmd=Query&ORT=77&Parameter=" + parameter + "&Hauptreihe=true", requests.getFirst());
        assertEquals("Cmd=PUT&ZRID=" + parameter + "&QUAL=0", requests.getLast());

        var xml = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(writes.get(parameter)));
        assertEquals(unit, xml.getElementsByTagName("DEF").item(0).getAttributes().getNamedItem("EINHEIT").getNodeValue());
        byte[] binary = Base64.getMimeDecoder().decode(xml.getElementsByTagName("DATA").item(0).getTextContent());
        assertEquals(value, (double) ByteBuffer.wrap(binary, 8, 4).getFloat());
        assertEquals(OBSERVED_AT, new TstpBinaryCodec().decode(binary).getFirst().getObservedAt());
    }

    @Test
    void cachesEachStationParameterIndependentlyAndRechecksUnitsOnCacheHits() {
        registerSeries("Wasserstand", "cm", 42);
        registerSeries("WTemperatur", "\u00b0C", 17);
        registerSeries("Abfluss", "l/s", 1250);
        var resolver = new TstpCatalogResolver(client);

        for (int cycle = 0; cycle < 2; cycle++) {
            assertEquals("Wasserstand", resolver.resolveZrid(77, TstpParameter.WATER_LEVEL, "cm"));
            assertEquals("WTemperatur", resolver.resolveZrid(77, TstpParameter.WATER_TEMPERATURE, "\u00b0C"));
            assertEquals("Abfluss", resolver.resolveZrid(77, TstpParameter.DISCHARGE, "l/s"));
        }
        assertEquals(3, requests.size());
        assertThrows(IllegalStateException.class,
                () -> resolver.resolveZrid(77, TstpParameter.DISCHARGE, "m3/s"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"empty", "ambiguous", "station", "parameter", "unit", "missing-unit", "zrid",
            "non-main", "missing-main"})
    void rejectsUnusableCatalogsBeforeReadingOrWritingAndRetriesAfterCorrection(String fault) {
        registerSeries("Abfluss", "l/s", 1250);
        String valid = catalogs.get("Abfluss");
        String invalid = switch (fault) {
            case "empty" -> "<TSQ/>";
            case "ambiguous" -> valid.replace("</TSQ>", valid.substring(valid.indexOf("<TSATTR>")));
            case "station" -> valid.replace("<ORT>77</ORT>", "<ORT>78</ORT>");
            case "parameter" -> valid.replace("<PARAMETER>Abfluss</PARAMETER>", "<PARAMETER>Wasserstand</PARAMETER>");
            case "unit" -> valid.replace("<EINHEIT>l/s</EINHEIT>", "<EINHEIT>m3/s</EINHEIT>");
            case "missing-unit" -> valid.replace("<EINHEIT>l/s</EINHEIT>", "");
            case "zrid" -> valid.replace("<ZRID>Abfluss</ZRID>", "<ZRID> </ZRID>");
            case "non-main" -> valid.replace("<HAUPTREIHE>T</HAUPTREIHE>", "<HAUPTREIHE>F</HAUPTREIHE>");
            case "missing-main" -> valid.replace("<HAUPTREIHE>T</HAUPTREIHE>", "");
            default -> throw new AssertionError(fault);
        };
        catalogs.put("Abfluss", invalid);
        var resolver = new TstpCatalogResolver(client);
        var inbound = new TstpMapping(SERIES, 77, MappingDirection.EXTERNAL_TO_CORE, TstpParameter.DISCHARGE, "l/s");
        var outbound = new TstpMapping(SERIES, 77, MappingDirection.CORE_TO_EXTERNAL, TstpParameter.DISCHARGE, "l/s");

        synchronize(inbound, resolver);
        synchronize(outbound, resolver);

        verify(core, never()).sendMeasurements(any());
        verify(core, never()).getMeasurementsOfTimeSeries(any(), any(), any(), any());
        assertEquals(2, requests.size());
        assertEquals(0, writes.size());

        catalogs.put("Abfluss", valid);
        synchronize(inbound, resolver);
        verify(core).sendMeasurements(any());
        assertEquals(3, requests.stream().filter(q -> q.startsWith("Cmd=Query&")).count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"wrong-unit", "missing-unit", "missing-def"})
    void rejectsMeasurementUnitMismatchEvenWhenTheCatalogWasCorrect(String fault) {
        registerSeries("WTemperatur", "\u00b0C", 17);
        String valid = new String(readings.get("WTemperatur"), StandardCharsets.ISO_8859_1);
        String invalid = switch (fault) {
            case "wrong-unit" -> valid.replace("EINHEIT=\"\u00b0C\"", "EINHEIT=\"cm\"");
            case "missing-unit" -> valid.replace("EINHEIT=\"\u00b0C\"", "");
            case "missing-def" -> valid.replaceFirst("<DEF[^>]*/>", "");
            default -> throw new AssertionError(fault);
        };
        readings.put("WTemperatur", invalid.getBytes(StandardCharsets.ISO_8859_1));

        synchronize(new TstpMapping(SERIES, 77, MappingDirection.EXTERNAL_TO_CORE,
                TstpParameter.WATER_TEMPERATURE, "\u00b0C"), new TstpCatalogResolver(client));

        verify(core, never()).sendMeasurements(any());
    }

    private void synchronize(TstpMapping mapping, TstpCatalogResolver resolver) {
        new TstpSynchronizer(core, client, resolver, List.of(mapping), Duration.ofMinutes(15), CLOCK).run();
    }

    private void registerSeries(String parameter, String unit, double value) {
        catalogs.put(parameter, """
                <?xml version="1.0" encoding="ISO-8859-1"?>
                <TSQ><TSATTR><ZRID>%s</ZRID><ORT>77</ORT><PARAMETER>%s</PARAMETER>
                <EINHEIT>%s</EINHEIT><HAUPTREIHE>T</HAUPTREIHE></TSATTR></TSQ>
                """.formatted(parameter, parameter, unit));
        byte[] point = ByteBuffer.allocate(12).put(HexFormat.of().parseHex("0007ea06070a0f1e"))
                .putFloat((float) value).array();
        readings.put(parameter, """
                <?xml version="1.0" encoding="ISO-8859-1"?>
                <TSD RELEASE="1"><DEF REIHENART="Z" TEXT="Nein" DEFART="K" EINHEIT="%s" LEN="12" ANZ="1"/>
                <DATA>%s</DATA></TSD>
                """.formatted(unit, Base64.getEncoder().encodeToString(point)).getBytes(StandardCharsets.ISO_8859_1));
    }
}
