package at.pegelhub.lib.internal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import at.pegelhub.lib.PegelHubCommunicator;
import at.pegelhub.lib.exception.NotFoundException;
import at.pegelhub.lib.internal.dto.*;
import at.pegelhub.lib.internal.gsonconverters.InstantConverter;
import at.pegelhub.lib.model.*;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.HttpEntities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class HttpPegelHubCommunicator implements PegelHubCommunicator {

    private static final Logger LOG = LoggerFactory.getLogger(HttpPegelHubCommunicator.class);
    private final String measurementRoute;
    private final String telemetryRoute;
    private final String contactRoute;
    private final String connectorRoute;
    private final String takerRoute;
    private final String supplierRoute;
    private final URL baseUrl;
    private final CloseableHttpClient client;
    private final ApplicationProperties properties;
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

    private boolean isOAuthConfigured() {
        return hasText(properties.getTokenUrl()) && hasText(properties.getClientId()) && hasText(properties.getClientSecret());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
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
            var http = new HttpPost(URI.create(properties.getTokenUrl()));
            http.setHeader("Content-Type", "application/x-www-form-urlencoded");
            List<NameValuePair> form = List.of(
                    new BasicNameValuePair("grant_type", "client_credentials"),
                    new BasicNameValuePair("client_id", properties.getClientId()),
                    new BasicNameValuePair("client_secret", properties.getClientSecret()));
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

    private void ensureIsTaker() {
        if (properties.isSupplier()) throw new IllegalArgumentException();
    }

    private void ensureIsSupplier() {
        if (!properties.isSupplier()) throw new IllegalArgumentException();
    }

    private void sendMetaData() {
        if (properties.isSupplier()) {
            sendSupplierData();
        } else {
            sendTakerData();
        }
    }

    private void sendSupplierData() {
        try {
            final URI uri = baseUrl.toURI().resolve(supplierRoute);
            final var http = new HttpPost(uri);
            authorize(http);
            http.setHeader("Content-Type", "application/json");

            LOG.debug("Summertime: " + properties.getSupplier().isSummertime());


            var gson = new Gson();
            var json = gson.toJson(properties.getSupplier(), SupplierSendDto.class);
            var entity = HttpEntities.create(json);
            http.setEntity(entity);

            boolean result = client.<Boolean>execute(http, response -> {
                EntityUtils.consume(response.getEntity());
                return response.getCode() == HttpStatus.SC_OK;
            });
            if (!result) {
                throw new RuntimeException("Invalid request");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void sendTakerData() {
        try {
            final URI uri = baseUrl.toURI().resolve(takerRoute);
            final var http = new HttpPost(uri);
            authorize(http);
            http.setHeader("Content-Type", "application/json");

            var gson = new Gson();
            var json = gson.toJson(properties.getTaker(), TakerSendDto.class);
            var entity = HttpEntities.create(json);
            http.setEntity(entity);

            boolean result = client.<Boolean>execute(http, response -> {
                EntityUtils.consume(response.getEntity());
                LOG.info(response.getCode() + "");
                return response.getCode() == HttpStatus.SC_OK;
            });
            if (!result) {
                throw new RuntimeException("Invalid request");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public HttpPegelHubCommunicator(CloseableHttpClient client, URL baseUrl, ApplicationProperties properties) {
        this(client, baseUrl, properties,
                "api/v1/measurement",
                "api/v1/telemetry",
                "api/v1/contact",
                "api/v1/connector",
                "api/v1/taker",
                "api/v1/supplier");
    }

    public HttpPegelHubCommunicator(CloseableHttpClient client, URL baseUrl, ApplicationProperties properties, String measurementRoute, String telemetryRoute, String contactRoute, String connectorRoute, String takerRoute, String supplierRoute) {
        this.client = client;
        this.baseUrl = baseUrl;
        this.measurementRoute = measurementRoute;
        this.telemetryRoute = telemetryRoute;
        this.contactRoute = contactRoute;
        this.connectorRoute = connectorRoute;
        this.takerRoute = takerRoute;
        this.supplierRoute = supplierRoute;
        this.properties = properties;
        requireOAuthConfiguration();
        if (properties.isSupplierDataToSend()) {
            sendMetaData();
        }
    }

    private void requireOAuthConfiguration() {
        if (!isOAuthConfigured()) {
            throw new IllegalStateException(
                    "Missing Keycloak client credentials configuration. " +
                            "Please configure keycloak.tokenUrl, keycloak.clientId, and keycloak.clientSecret.");
        }
    }

    @Override
    public Collection<Measurement> getMeasurements(String timespan) {
        try {
            ensureIsTaker();

            final URI uri = baseUrl.toURI().resolve(measurementRoute + "/" + timespan);
            final var http = new HttpGet(uri);
            authorize(http);

            return client.execute(http, response -> {
                var json = EntityUtils.toString(response.getEntity());
                var gson = gsonWithInstantSupport();
                var listType = new TypeToken<List<MeasurementReceiveDto>>() {
                };
                List<MeasurementReceiveDto> measurements = gson.fromJson(json, listType);
                return measurements.stream()
                        .map(MeasurementReceiveDto::toMeasurement)
                        .toList();
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Measurement> getMeasurementByUUID(UUID uuid) {
        try {
            ensureIsTaker();

            final URI uri = baseUrl.toURI().resolve(measurementRoute + "/last/" + uuid);
            final var http = new HttpGet(uri);
            authorize(http);

            return Optional.ofNullable(client.execute(http, response -> {
                if (response.getCode() != HttpStatus.SC_OK) {
                    throw new RuntimeException("Invalid status: " + response.getCode());
                }

                var json = EntityUtils.toString(response.getEntity());
                var gson = gsonWithInstantSupport();
                return gson.fromJson(json, MeasurementReceiveDto.class).toMeasurement();
            }));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Collection<Measurement> getMeasurementsOfTimeSeries(UUID timeSeriesId, String timespan) {
        try {
            ensureIsTaker();

            final URI uri = baseUrl.toURI().resolve(measurementRoute + "/time-series/" + timeSeriesId + "/" + timespan);
            final var http = new HttpGet(uri);
            authorize(http);

            return client.execute(http, response -> {
                if (response.getCode() == 404) {
                    throw new NotFoundException("time series does not exist");
                }
                var json = EntityUtils.toString(response.getEntity());
                var gson = gsonWithInstantSupport();
                var listType = new TypeToken<List<MeasurementReceiveDto>>() {
                };
                List<MeasurementReceiveDto> measurements = gson.fromJson(json, listType);
                return measurements.stream()
                        .map(MeasurementReceiveDto::toMeasurement)
                        .toList();
            });
        } catch (NotFoundException nfe) {
            throw new NotFoundException(nfe.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Measurement> getLatestMeasurementOfTimeSeries(UUID timeSeriesId) {
        try {
            ensureIsTaker();

            final URI uri = baseUrl.toURI().resolve(measurementRoute + "/time-series/" + timeSeriesId + "/latest");
            final var http = new HttpGet(uri);
            authorize(http);

            return Optional.ofNullable(client.execute(http, response -> {
                if (response.getCode() != HttpStatus.SC_OK) {
                    return null;
                }

                var json = EntityUtils.toString(response.getEntity());
                var gson = gsonWithInstantSupport();
                return gson.fromJson(json, MeasurementReceiveDto.class).toMeasurement();
            }));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Collection<Telemetry> getTelemetry(String timespan) {
        try {
            ensureIsTaker();
            final URI uri = baseUrl.toURI().resolve(telemetryRoute + "/" + timespan);
            final var http = new HttpGet(uri);
            authorize(http);
            LOG.debug("Executing GET request to URI: {}", uri);

            return client.execute(http, response -> {
                var json = EntityUtils.toString(response.getEntity());
                var gson = new Gson();
                var mapType = new TypeToken<Map<String, Map<String, TelemetryCollectionReceiveDto.TelemetryReceiveDtoInnerType>>>() {
                };

                return new TelemetryCollectionReceiveDto(gson.fromJson(json, mapType)).toTelemetryCollection();
            });
        } catch (Exception e) {
            LOG.error("Exception during telemetry fetch: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Telemetry> getTelemetryByUUID(UUID uuid) {
        try {
            ensureIsTaker();
            final URI uri = baseUrl.toURI().resolve(telemetryRoute + "/last/" + uuid);
            final var http = new HttpGet(uri);
            authorize(http);

            return client.execute(http, response -> {
                if (response.getCode() != HttpStatus.SC_OK) {
                    throw new RuntimeException("Invalid status: " + response.getCode());
                }
                var json = EntityUtils.toString(response.getEntity());
                var gson = new Gson();
                var mapType = new TypeToken<Map<String, Map<String, TelemetryCollectionReceiveDto.TelemetryReceiveDtoInnerType>>>() {
                };
                var telemetry = new TelemetryCollectionReceiveDto(gson.fromJson(json, mapType)).toTelemetryCollection();
                Optional<Telemetry> returnValue = telemetry.stream().findFirst();
                return returnValue;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendMeasurements(List<Measurement> meass) {
        try {
            ensureIsSupplier();

            final URI uri = baseUrl.toURI().resolve(measurementRoute);
            final var http = new HttpPost(uri);
            authorize(http);
            http.setHeader("Content-Type", "application/json");
            var dto = new MeasurementsSendDto(meass.stream().map(this::toMeasurementSendDto).toList());
            var gson = gsonWithInstantSupport();
            var json = gson.toJson(dto, MeasurementsSendDto.class);
            var entity = HttpEntities.create(json);
            http.setEntity(entity);

            boolean result = client.<Boolean>execute(http, response -> {
                EntityUtils.consume(response.getEntity());
                return response.getCode() == HttpStatus.SC_OK;
            });
            if (!result) {
                throw new RuntimeException("Invalid request");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
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
    public void sendTelemetry(Telemetry tel) {
        try {
            ensureIsSupplier();

            final URI uri = baseUrl.toURI().resolve(telemetryRoute);
            final var http = new HttpPost(uri);
            authorize(http);
            http.setHeader("Content-Type", "application/json");

            var gson = gsonWithInstantSupport();
            var json = gson.toJson(tel, Telemetry.class);
            var entity = HttpEntities.create(json);
            http.setEntity(entity);

            boolean result = client.<Boolean>execute(http, response -> {
                EntityUtils.consume(response.getEntity());
                return response.getCode() == HttpStatus.SC_OK;
            });
            if (!result) {
                throw new RuntimeException("Invalid request");
            }
        } catch (Exception e) {
            LOG.debug(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public Collection<Connector> getConnectors() {
        try {

            final URI uri = baseUrl.toURI().resolve(connectorRoute);
            final var http = new HttpGet(uri);
            authorize(http);

            return client.execute(http, response -> {
                var json = EntityUtils.toString(response.getEntity());
                var gson = new Gson();
                var mapType = new TypeToken<Collection<ConnectorReceiveDto>>() {
                };
                var cons = gson.fromJson(json, mapType);
                return cons.stream().map(ConnectorReceiveDto::toConnector).collect(Collectors.toList());
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Connector> getConnectorByUUID(UUID uuid) {
        try {

            final URI uri = baseUrl.toURI().resolve(connectorRoute + "/" + uuid);
            final var http = new HttpGet(uri);
            authorize(http);

            return client.execute(http, response -> {
                if (response.getCode() != HttpStatus.SC_OK) {
                    throw new RuntimeException("Invalid status: " + response.getCode());
                }
                var json = EntityUtils.toString(response.getEntity());
                var gson = new Gson();
                var mapType = new TypeToken<ConnectorReceiveDto>() {
                };
                var connector = gson.fromJson(json, mapType);
                if (connector == null) {
                    return Optional.empty();
                }
                return Optional.of(connector.toConnector());
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendConnector(Connector connector) {
        try {

            final URI uri = baseUrl.toURI().resolve(connectorRoute);
            final var http = new HttpPost(uri);
            authorize(http);
            http.setHeader("Content-Type", "application/json");

            var dto = new ConnectorSendDto(null, connector.getManufacturerId(), connector.getTypeDescription(), Double.toString(connector.getSoftwareVersion()), Double.toString(connector.getWorksFromDataVersion()), connector.getDataDefinition(), connector.getSoftwareManufacturerId(), connector.getTechnicallyResponsibleId(), connector.getOperatingCompanyId(), connector.getNodes());
            var gson = new Gson();
            var json = gson.toJson(dto, ConnectorSendDto.class);
            var entity = HttpEntities.create(json);
            http.setEntity(entity);

            boolean result = client.<Boolean>execute(http, response -> {
                EntityUtils.consume(response.getEntity());
                return response.getCode() == HttpStatus.SC_OK;
            });
            if (!result) {
                throw new RuntimeException("Invalid request");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Collection<Contact> getContacts() {
        try {
            final URI uri = baseUrl.toURI().resolve(contactRoute);
            final var http = new HttpGet(uri);
            authorize(http);

            return client.execute(http, response -> {
                var json = EntityUtils.toString(response.getEntity());
                var gson = new Gson();
                var mapType = new TypeToken<Collection<Contact>>() {
                };
                return gson.fromJson(json, mapType);
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Contact> getContactByUUID(UUID uuid) {
        try {
            final URI uri = baseUrl.toURI().resolve(contactRoute + "/" + uuid);
            final var http = new HttpGet(uri);
            authorize(http);

            return client.execute(http, response -> {
                if (response.getCode() != HttpStatus.SC_OK) {
                    throw new RuntimeException("Invalid status: " + response.getCode());
                }
                var json = EntityUtils.toString(response.getEntity());
                var gson = new Gson();
                var mapType = new TypeToken<Contact>() {
                };
                var contact = gson.fromJson(json, mapType);
                if (contact == null) {
                    return Optional.empty();
                }
                return Optional.of(contact);
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendContact(Contact contact) {
        try {
            final URI uri = baseUrl.toURI().resolve(connectorRoute);
            final var http = new HttpPost(uri);
            authorize(http);
            http.setHeader("Content-Type", "application/json");

            var gson = new Gson();
            var json = gson.toJson(contact, Contact.class);
            var entity = HttpEntities.create(json);
            http.setEntity(entity);

            boolean result = client.<Boolean>execute(http, response -> {
                EntityUtils.consume(response.getEntity());
                return response.getCode() == HttpStatus.SC_OK;
            });
            if (!result) {
                throw new RuntimeException("Invalid request");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
        client.close();
    }

    @Override
    public Collection<Supplier> getSuppliers() {
        try {
            final URI uri = baseUrl.toURI().resolve(supplierRoute);
            final var http = new HttpGet(uri);
            authorize(http);
            return client.execute(http, response -> {
                var json = EntityUtils.toString(response.getEntity());
                var gson = new Gson();
                var mapType = new TypeToken<Collection<Supplier>>() {
                };
                return gson.fromJson(json, mapType);
            });
        } catch (Exception e) {
            LOG.error(e + "");
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Supplier> getSupplierbyUUID(UUID uuid) {
        Collection<Supplier> suppliers = getSuppliers();

        if (uuid != null) {
            String id = uuid.toString();
            Set<Supplier> supplierSet = suppliers.stream().filter(s -> id.equals(s.getId())).collect(Collectors.toSet());

            if (supplierSet.size() == 1) {
                return supplierSet.stream().findFirst();
            } else {
                throw new NotFoundException("No supplier found");
            }
        } else {
            throw new IllegalArgumentException("UUID cannot be null!");
        }


    }

    public Instant getSystemTime() {
        try {
            final URI uri = baseUrl.toURI().resolve(measurementRoute + "/systemTime");
            final var http = new HttpGet(uri);
            return client.execute(http, response -> {
                var json = EntityUtils.toString(response.getEntity());
                return gsonWithInstantSupport().fromJson(json, Instant.class);
            });
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public UUID getConnectorID(UUID uuid) {
        try {
            final URI uri = baseUrl.toURI().resolve(supplierRoute + "/connectorID/" + uuid.toString());
            LOG.debug(uri + "");
            final var http = new HttpGet(uri);
            authorize(http);
            return client.execute(http, response -> {
                var json = EntityUtils.toString(response.getEntity());
                var gson = new Gson();
                var mapType = new TypeToken<UUID>() {
                };
                return gson.fromJson(json, mapType);
            });
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
