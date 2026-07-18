package at.pegelhub.lib.internal;

import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.config.CoreAuthentication;
import at.pegelhub.lib.exception.NotFoundException;
import at.pegelhub.lib.internal.dto.*;
import at.pegelhub.lib.internal.gsonconverters.InstantConverter;
import at.pegelhub.lib.model.Measurement;
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
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class HttpPegelHubClient implements PegelHubClient {

    private static final String LATEST_MEASUREMENT_WINDOW = "365d";
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
                    throw new RuntimeException("Token request failed with status: " + response.getCode());
                }
                JsonObject json = JsonParser.parseString(EntityUtils.toString(response.getEntity())).getAsJsonObject();
                accessToken = json.get("access_token").getAsString();
                long expiresIn = json.has("expires_in") ? json.get("expires_in").getAsLong() : 60L;
                accessTokenExpiresAt = Instant.now().plusSeconds(Math.max(1L, expiresIn));
                return accessToken;
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public HttpPegelHubClient(CloseableHttpClient client, URL baseUrl, CoreAuthentication authentication) {
        this.client = client;
        this.baseUrl = baseUrl;
        this.measurementRoute = "api/v1/measurements";
        this.authentication = Objects.requireNonNull(authentication, "authentication");
    }

    @Override
    public Collection<Measurement> getMeasurementsOfTimeSeries(UUID timeSeriesId, Duration lookback) {
        try {
            final URI uri = measurementsUri(timeSeriesId, "last=" + urlEncode(durationLiteral(lookback)));
            final var http = new HttpGet(uri);
            authorize(http);

            return client.execute(http, response -> {
                if (response.getCode() == 404) {
                    EntityUtils.consume(response.getEntity());
                    throw new NotFoundException("time series does not exist");
                }
                requireOk(response.getCode(), response.getEntity());
                var json = EntityUtils.toString(response.getEntity());
                var gson = gsonWithInstantSupport();
                var result = gson.fromJson(json, MeasurementListReceiveDto.class);
                if (result.truncated()) {
                    throw new IllegalStateException(
                            "Core truncated the measurement lookback for time series " + timeSeriesId);
                }
                return result.toMeasurements(timeSeriesId);
            });
        } catch (NotFoundException nfe) {
            throw new NotFoundException(nfe.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String durationLiteral(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("lookback must be positive");
        }
        return duration.toSeconds() + "s";
    }

    @Override
    public Optional<Measurement> getLatestMeasurementOfTimeSeries(UUID timeSeriesId) {
        try {
            final URI uri = measurementsUri(timeSeriesId, "last=" + LATEST_MEASUREMENT_WINDOW + "&order=desc&limit=1");
            final var http = new HttpGet(uri);
            authorize(http);

            return Optional.ofNullable(client.execute(http, response -> {
                if (response.getCode() == 404) {
                    EntityUtils.consume(response.getEntity());
                    throw new NotFoundException("time series does not exist");
                }
                requireOk(response.getCode(), response.getEntity());

                var json = EntityUtils.toString(response.getEntity());
                var gson = gsonWithInstantSupport();
                return gson.fromJson(json, MeasurementListReceiveDto.class).toMeasurements(timeSeriesId).stream()
                        .findFirst()
                        .orElse(null);
            }));
        } catch (NotFoundException nfe) {
            throw nfe;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendMeasurements(List<Measurement> measurements) {
        try {
            final URI uri = baseUrl.toURI().resolve(measurementRoute);
            final var http = new HttpPost(uri);
            authorize(http);
            http.setHeader("Content-Type", "application/json");
            var dto = new MeasurementsSendDto(measurements.stream().map(this::toMeasurementSendDto).toList());
            var gson = gsonWithInstantSupport();
            var json = gson.toJson(dto, MeasurementsSendDto.class);
            var entity = HttpEntities.create(json);
            http.setEntity(entity);

            boolean result = client.<Boolean>execute(http, response -> {
                EntityUtils.consume(response.getEntity());
                return response.getCode() == HttpStatus.SC_OK || response.getCode() == HttpStatus.SC_NO_CONTENT;
            });
            if (!result) {
                throw new RuntimeException("Invalid request");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private URI measurementsUri(UUID timeSeriesId, String query) throws URISyntaxException {
        return baseUrl.toURI().resolve("api/v1/time-series/" + timeSeriesId + "/measurements?" + query);
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static void requireOk(int statusCode, org.apache.hc.core5.http.HttpEntity entity) throws IOException {
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
        return new MeasurementSendDto(measurement.getTimeSeriesId(), measurement.getObservedAt(), measurement.getValue());
    }

    @Override
    public void close() throws IOException {
        client.close();
    }

}
