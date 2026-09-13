package at.pegelhub.lib.internal;

import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.config.CoreAuthentication;
import at.pegelhub.lib.exception.NotFoundException;
import at.pegelhub.lib.internal.dto.MeasurementListReceiveDto;
import at.pegelhub.lib.internal.dto.MeasurementSendDto;
import at.pegelhub.lib.internal.dto.MeasurementsSendDto;
import at.pegelhub.lib.internal.gsonconverters.InstantConverter;
import at.pegelhub.lib.model.Measurement;
import at.pegelhub.lib.model.MeasurementRepresentation;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.HttpEntities;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class HttpPegelHubClient implements PegelHubClient {

    private static final String LATEST_MEASUREMENT_WINDOW = "365d";
    private static final int SYNCHRONIZATION_READ_LIMIT = 10_000;
    private final String measurementRoute;
    private final URL baseUrl;
    private final CloseableHttpClient client;
    private final CoreAuthentication authentication;
    private String accessToken;
    private Instant accessTokenExpiresAt;

    private void authorize(HttpUriRequestBase request) {
        request.setHeader("Authorization", "Bearer " + bearerToken());
    }

    private String bearerToken() {
        if (accessToken != null
                && accessTokenExpiresAt != null
                && accessTokenExpiresAt.minusSeconds(30).isAfter(Instant.now())) {
            return accessToken;
        }
        return fetchAccessToken();
    }

    private static Gson gsonWithInstantSupport() {
        return new GsonBuilder()
                .registerTypeAdapter(Instant.class, new InstantConverter())
                .create();
    }

    private synchronized String fetchAccessToken() {
        if (accessToken != null
                && accessTokenExpiresAt != null
                && accessTokenExpiresAt.minusSeconds(30).isAfter(Instant.now())) {
            return accessToken;
        }
        try {
            var http = new HttpPost(URI.create(authentication.tokenUrl()));
            http.setHeader("Content-Type", "application/x-www-form-urlencoded");
            List<NameValuePair> form = List.of(
                    new BasicNameValuePair("grant_type", "client_credentials"),
                    new BasicNameValuePair("client_id", authentication.clientId()),
                    new BasicNameValuePair("client_secret", authentication.clientSecret()));
            http.setEntity(new UrlEncodedFormEntity(form, StandardCharsets.UTF_8));

            return client.execute(http, response -> {
                if (response.getCode() != HttpStatus.SC_OK) {
                    EntityUtils.consume(response.getEntity());
                    throw new RuntimeException(
                            "Token request failed with status: " + response.getCode());
                }
                JsonObject json = JsonParser
                        .parseString(EntityUtils.toString(response.getEntity()))
                        .getAsJsonObject();
                accessToken = json.get("access_token").getAsString();
                long expiresIn = json.has("expires_in") ? json.get("expires_in").getAsLong() : 60L;
                accessTokenExpiresAt = Instant.now().plusSeconds(Math.max(1L, expiresIn));
                return accessToken;
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public HttpPegelHubClient(
            CloseableHttpClient client,
            URL baseUrl,
            CoreAuthentication authentication) {
        this.client = client;
        this.baseUrl = baseUrl;
        this.measurementRoute = "api/v1/measurements";
        this.authentication = Objects.requireNonNull(authentication, "authentication");
    }

    @Override
    public Collection<Measurement> getMeasurementsOfTimeSeries(
            UUID timeSeriesId,
            Instant from,
            Instant to,
            MeasurementRepresentation representation) {
        Objects.requireNonNull(representation, "representation");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (!to.isAfter(from)) {
            throw new IllegalArgumentException("to must be after from");
        }

        try {
            List<Measurement> measurements = new ArrayList<>();
            readMeasurementWindow(timeSeriesId, from, to, representation, measurements);
            return measurements;
        } catch (NotFoundException nfe) {
            throw nfe;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void readMeasurementWindow(
            UUID timeSeriesId,
            Instant from,
            Instant to,
            MeasurementRepresentation representation,
            List<Measurement> measurements)
            throws IOException, URISyntaxException {
        String query = "from=" + urlEncode(from.toString())
                + "&to=" + urlEncode(to.toString())
                + "&order=asc&limit=" + SYNCHRONIZATION_READ_LIMIT;
        MeasurementListReceiveDto page = readMeasurementPage(timeSeriesId, query, representation);
        if (!page.truncated()) {
            measurements.addAll(page.toMeasurements());
            return;
        }

        // Discard the partial page, then read both halves. The API has no next-page cursor.
        // The end time is excluded, so measurements at the split belong only to the second half.
        Instant middle = from.plus(Duration.between(from, to).dividedBy(2));
        if (!middle.isAfter(from) || !middle.isBefore(to)) {
            throw new IllegalStateException(
                    "Core truncated an indivisible synchronization window for time series " + timeSeriesId);
        }

        readMeasurementWindow(timeSeriesId, from, middle, representation, measurements);
        readMeasurementWindow(timeSeriesId, middle, to, representation, measurements);
    }

    /**
     * Searches the last {@value #LATEST_MEASUREMENT_WINDOW} and returns values in the requested representation.
     * An empty result does not mean the series has no older data.
     */
    @Override
    public Optional<Measurement> getLatestMeasurementOfTimeSeries(
            UUID timeSeriesId,
            MeasurementRepresentation representation) {
        Objects.requireNonNull(representation, "representation");
        try {
            String query = "last=" + LATEST_MEASUREMENT_WINDOW + "&order=desc&limit=1";
            var page = readMeasurementPage(timeSeriesId, query, representation);
            // We only asked for the newest value, so a truncated response is fine here.
            return page.toMeasurements().stream().findFirst();
        } catch (NotFoundException nfe) {
            throw nfe;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private MeasurementListReceiveDto readMeasurementPage(
            UUID timeSeriesId,
            String query,
            MeasurementRepresentation representation) throws IOException, URISyntaxException {
        String representedQuery = query + "&representation=" + representation.value();
        HttpGet http = new HttpGet(measurementsUri(timeSeriesId, representedQuery));
        authorize(http);

        return client.execute(http, response -> {
            if (response.getCode() == HttpStatus.SC_NOT_FOUND) {
                EntityUtils.consume(response.getEntity());
                throw new NotFoundException("time series does not exist");
            }
            requireOk(response.getCode(), response.getEntity());

            String json = EntityUtils.toString(response.getEntity());
            var page = gsonWithInstantSupport().fromJson(json, MeasurementListReceiveDto.class);
            if (page == null) {
                throw new IllegalStateException("Core returned an empty measurement response");
            }
            page.requireMatches(timeSeriesId, representation);
            return page;
        });
    }

    @Override
    public void sendMeasurements(List<Measurement> measurements) {
        try {
            final URI uri = baseUrl.toURI().resolve(measurementRoute);
            final var http = new HttpPost(uri);
            authorize(http);
            http.setHeader("Content-Type", "application/json");
            var dto = new MeasurementsSendDto(
                    measurements.stream().map(this::toMeasurementSendDto).toList());
            var gson = gsonWithInstantSupport();
            var json = gson.toJson(dto, MeasurementsSendDto.class);
            var entity = HttpEntities.create(json);
            http.setEntity(entity);

            boolean result = client.<Boolean>execute(http, response -> {
                EntityUtils.consume(response.getEntity());
                return response.getCode() == HttpStatus.SC_OK
                        || response.getCode() == HttpStatus.SC_NO_CONTENT;
            });
            if (!result) {
                throw new RuntimeException("Invalid request");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private URI measurementsUri(UUID timeSeriesId, String query) throws URISyntaxException {
        String path = "api/v1/time-series/" + timeSeriesId + "/measurements?" + query;
        return baseUrl.toURI().resolve(path);
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static void requireOk(
            int statusCode,
            org.apache.hc.core5.http.HttpEntity entity) throws IOException {
        if (statusCode != HttpStatus.SC_OK) {
            EntityUtils.consume(entity);
            throw new IOException("Core request failed with status: " + statusCode);
        }
    }

    private MeasurementSendDto toMeasurementSendDto(Measurement measurement) {
        if (measurement.getTimeSeriesId() == null) {
            throw new IllegalArgumentException("Measurement timeSeriesId must be set");
        }
        if (measurement.getObservedAt() == null) {
            throw new IllegalArgumentException("Measurement observedAt must be set");
        }
        if (measurement.getValue() == null) {
            throw new IllegalArgumentException("Measurement value must be set");
        }
        return new MeasurementSendDto(
                measurement.getTimeSeriesId(),
                measurement.getObservedAt(),
                measurement.getValue());
    }

    @Override
    public void close() throws IOException {
        client.close();
    }

}
