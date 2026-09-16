package at.pegelhub.connector.tstp.client;

import at.pegelhub.connector.tstp.TstpParameter;
import at.pegelhub.connector.tstp.codec.TstpBinaryCodec;
import at.pegelhub.connector.tstp.codec.TstpXmlCodec;
import at.pegelhub.connector.tstp.config.TstpServer;
import at.pegelhub.connector.tstp.service.model.XmlQueryResponse;
import at.pegelhub.connector.tstp.service.model.XmlTsResponse;
import at.pegelhub.lib.config.ConfigValidation;
import at.pegelhub.lib.model.Measurement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class HttpTstpClient implements TstpClient {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final Logger LOG = LoggerFactory.getLogger(HttpTstpClient.class);
    private static final DateTimeFormatter TSTP_TIME = DateTimeFormatter
            .ofPattern("uuuu-MM-dd'T'HH:mm:ss'Z'");

    private final URI endpoint;
    private final HttpClient httpClient;
    private final TstpXmlCodec xmlCodec;
    private final Duration requestTimeout;
    private final DateTimeFormatter timeFormat;

    public static HttpTstpClient open(TstpServer server) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();

        ZoneOffset offset = ZoneOffset.of(server.timeOffset());
        return new HttpTstpClient(server.host(), server.port(), httpClient,
                new TstpXmlCodec(new TstpBinaryCodec(offset)), REQUEST_TIMEOUT, offset);
    }

    HttpTstpClient(
            String address,
            int port,
            HttpClient httpClient,
            TstpXmlCodec xmlCodec,
            Duration requestTimeout,
            ZoneOffset timeOffset) {
        this.endpoint = URI.create("http://" + address + ":" + port + "/");
        this.httpClient = httpClient;
        this.xmlCodec = xmlCodec;

        this.requestTimeout = ConfigValidation.requirePositive(requestTimeout, "requestTimeout");
        this.timeFormat = TSTP_TIME.withZone(timeOffset);
    }

    @Override
    public List<Measurement> readMeasurements(String zrid, Instant readFrom, Instant readUntil, String unit) {
        // Some servers interpolate both edges even with WERTE=True. Move them outside our logical window.
        URI uri = commandUri("Get&ZRID=" + zrid
                + "&Von=" + timeFormat.format(readFrom.minusSeconds(1))
                + "&Bis=" + timeFormat.format(readUntil.plusSeconds(1))
                + "&WERTE=True");

        LOG.debug("TSTP GET {}", uri);

        return xmlCodec.parseMeasurements(send(request(uri).GET().build(), "GET"), unit).stream()
                .filter(measurement -> !measurement.getObservedAt().isBefore(readFrom)
                        && measurement.getObservedAt().isBefore(readUntil))
                .toList();
    }

    @Override
    public XmlQueryResponse readCatalog(int stationId, TstpParameter parameter) {
        URI uri = commandUri("Query&ORT=" + stationId + "&Parameter=" + parameter.value() + "&Hauptreihe=true");

        LOG.debug("TSTP Query {}", uri);

        return xmlCodec.parseCatalog(send(request(uri).GET().build(), "Query"));
    }

    @Override
    public void writeMeasurements(String zrid, List<Measurement> measurements, String unit) {
        List<Measurement> sorted = new ArrayList<>(measurements);
        sorted.sort(Comparator.comparing(Measurement::getObservedAt));

        URI uri = commandUri("PUT&ZRID=" + zrid + "&QUAL=0");
        HttpRequest request = request(uri)
                .header("Content-Type", "text/xml; charset=ISO-8859-1")
                .POST(HttpRequest.BodyPublishers.ofString(xmlCodec.writeRequest(sorted, unit), StandardCharsets.ISO_8859_1))
                .build();

        XmlTsResponse response = xmlCodec.parseWriteResponse(send(request, "PUT"));

        if (response == null || response.getMessage() == null
                || !response.getMessage().trim().equalsIgnoreCase("confirm")) {
            throw new TstpClientException("TSTP PUT was not confirmed for ZRID " + zrid);
        }
    }

    @Override
    public void close() {
        httpClient.close();
    }

    private byte[] send(HttpRequest request, String command) {
        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new TstpClientException(
                        "TSTP " + command + " failed with HTTP status " + response.statusCode());
            }

            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TstpClientException("TSTP " + command + " was interrupted", e);
        } catch (IOException e) {
            throw new TstpClientException("TSTP " + command + " failed", e);
        }
    }

    private URI commandUri(String query) {
        return endpoint.resolve("?Cmd=" + query);
    }

    private HttpRequest.Builder request(URI uri) {
        return HttpRequest.newBuilder(uri).timeout(requestTimeout);
    }
}
