package at.pegelhub.connector.tstp.client;

import at.pegelhub.connector.tstp.codec.TstpXmlCodec;
import at.pegelhub.connector.tstp.service.model.XmlQueryResponse;
import at.pegelhub.connector.tstp.service.model.XmlTsResponse;
import at.pegelhub.lib.model.Measurement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class HttpTstpClient implements TstpClient {
    private static final Logger LOG = LoggerFactory.getLogger(HttpTstpClient.class);
    private static final DateTimeFormatter TSTP_TIME = DateTimeFormatter
            .ofPattern("uuuu-MM-dd'T'HH:mm:ss'Z'")
            .withZone(ZoneOffset.UTC);

    private final URI endpoint;
    private final HttpClient httpClient;
    private final TstpXmlCodec xmlCodec;

    public HttpTstpClient(String address, int port, HttpClient httpClient, TstpXmlCodec xmlCodec) {
        this.endpoint = URI.create("http://" + address + ":" + port + "/");
        this.httpClient = httpClient;
        this.xmlCodec = xmlCodec;
    }

    @Override
    public List<Measurement> readMeasurements(String zrid, Instant readFrom, Instant readUntil) {
        URI uri = commandUri("Get&ZRID=" + encode(zrid)
                + "&Von=" + encode(TSTP_TIME.format(readFrom))
                + "&Bis=" + encode(TSTP_TIME.format(readUntil)));
        LOG.debug("TSTP GET {}", uri);
        return xmlCodec.parseMeasurements(send(HttpRequest.newBuilder(uri).GET().build(), "GET"));
    }

    @Override
    public XmlQueryResponse readCatalog(int stationId) {
        URI uri = commandUri("Query&ORT=" + stationId + "&Parameter=Wasserstand&Hauptreihe=true");
        LOG.debug("TSTP Query {}", uri);
        return xmlCodec.parseCatalog(send(HttpRequest.newBuilder(uri).GET().build(), "Query"));
    }

    @Override
    public void writeMeasurements(String zrid, List<Measurement> measurements) {
        List<Measurement> sorted = new ArrayList<>(measurements);
        sorted.sort(Comparator.comparing(Measurement::getObservedAt));
        URI uri = commandUri("PUT&ZRID=" + encode(zrid));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .POST(HttpRequest.BodyPublishers.ofString(xmlCodec.writeRequest(sorted)))
                .build();
        XmlTsResponse response = xmlCodec.parseWriteResponse(send(request, "PUT"));
        if (response == null || response.getMessage() == null
                || !response.getMessage().toLowerCase().contains("confirm")) {
            throw new TstpClientException("TSTP PUT was not confirmed for ZRID " + zrid);
        }
    }

    @Override
    public void close() {
        httpClient.close();
    }

    private String send(HttpRequest request, String command) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
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

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
