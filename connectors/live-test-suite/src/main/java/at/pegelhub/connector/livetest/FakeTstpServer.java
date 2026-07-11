package at.pegelhub.connector.livetest;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

final class FakeTstpServer implements AutoCloseable {
    private final HarnessState state;
    private final HttpServer server;

    FakeTstpServer(HarnessState state) throws IOException {
        this.state = state;
        this.server = HttpServer.create(new InetSocketAddress("0.0.0.0", SuiteConstants.TSTP_PORT), 0);
        this.server.createContext("/", this::handle);
    }

    void start() {
        server.start();
    }

    private void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        Map<String, String> query = HttpSupport.query(exchange.getRequestURI().getRawQuery());
        String command = query.getOrDefault("Cmd", "");
        switch (command.toUpperCase()) {
            case "QUERY" -> handleQuery(exchange, query);
            case "GET" -> handleGet(exchange, query);
            case "PUT" -> handlePut(exchange, query);
            default -> HttpSupport.respondText(exchange, 400, "unknown TSTP command: " + command);
        }
    }

    private void handleQuery(com.sun.net.httpserver.HttpExchange exchange, Map<String, String> query) throws IOException {
        String station = query.get("ORT");
        state.tstpRequests.add(new TstpRequest(
                "Query",
                station,
                null,
                exchange.getRequestURI().getRawQuery(),
                "",
                List.of(),
                Instant.now()));
        String zrid = SuiteConstants.TSTP_READER_STATION_ID == Integer.parseInt(station)
                ? SuiteConstants.TSTP_READER_ZRID
                : SuiteConstants.TSTP_WRITER_ZRID;
        HttpSupport.respondText(exchange, 200, catalogXml(station, zrid));
    }

    private void handleGet(com.sun.net.httpserver.HttpExchange exchange, Map<String, String> query) throws IOException {
        String zrid = query.get("ZRID");
        state.tstpRequests.add(new TstpRequest(
                "Get",
                null,
                zrid,
                exchange.getRequestURI().getRawQuery(),
                "",
                List.of(),
                Instant.now()));
        HttpSupport.respondText(exchange, 200, tsDataXml(List.of(SuiteConstants.TSTP_READER_MEASUREMENT)));
    }

    private void handlePut(com.sun.net.httpserver.HttpExchange exchange, Map<String, String> query) throws IOException {
        String body = HttpSupport.readBody(exchange);
        List<MeasurementRecord> measurements = TstpBinaryCodec.decode(SuiteConstants.TSTP_WRITER_TS, extractBinary(body));
        state.tstpRequests.add(new TstpRequest(
                "PUT",
                null,
                query.get("ZRID"),
                exchange.getRequestURI().getRawQuery(),
                body,
                measurements,
                Instant.now()));
        HttpSupport.respondText(exchange, 200, "<TSR RELEASE=\"1\">\nconfirm</TSR>\n");
    }

    private static byte[] extractBinary(String xml) {
        int start = xml.indexOf("<DATA><![CDATA[");
        int end = xml.indexOf("]]></DATA>");
        if (start < 0 || end < 0 || end <= start) {
            return new byte[0];
        }
        String base64 = xml.substring(start + "<DATA><![CDATA[".length(), end).replaceAll("\\s+", "");
        return Base64.getDecoder().decode(base64);
    }

    private static String catalogXml(String station, String zrid) {
        return """
                <?xml version="1.0" encoding="ISO-8859-1"?>
                <TSQ RELEASE="1">
                  <TSATTR>
                    <ZRID>%s</ZRID>
                    <MAXFOCUS-Start>2010-08-03T13:30:00Z</MAXFOCUS-Start>
                    <MAXFOCUS-End>2026-06-25T13:30:00Z</MAXFOCUS-End>
                    <MAXQUAL>0</MAXQUAL>
                    <WRITABLE>True</WRITABLE>
                    <PARAMETER>Wasserstand</PARAMETER>
                    <ORT>%s</ORT>
                    <DEFART>K</DEFART>
                    <HERKUNFT>O</HERKUNFT>
                    <REIHENART>Z</REIHENART>
                    <EINHEIT>cm</EINHEIT>
                    <HAUPTREIHE>True</HAUPTREIHE>
                  </TSATTR>
                </TSQ>
                """.formatted(zrid, station);
    }

    private static String tsDataXml(List<MeasurementRecord> measurements) {
        byte[] binary = TstpBinaryCodec.encode(measurements);
        String base64 = Base64.getEncoder().encodeToString(binary);
        return """
                <TSD RELEASE="1">
                  <DEF REIHENART="Z" TEXT="Nein" DEFART="K" EINHEIT="cm" LEN="%d" ANZ="%d" />
                  <DATA><![CDATA[
                %s
                ]]></DATA>
                </TSD>
                """.formatted(binary.length, measurements.size(), base64);
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
