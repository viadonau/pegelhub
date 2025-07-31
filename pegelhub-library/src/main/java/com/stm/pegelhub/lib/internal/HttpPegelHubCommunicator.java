package com.stm.pegelhub.lib.internal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.stm.pegelhub.lib.PegelHubCommunicator;
import com.stm.pegelhub.lib.exception.NotFoundException;
import com.stm.pegelhub.lib.internal.dto.*;
import com.stm.pegelhub.lib.internal.gsonconverters.LocalDateTimeConverter;
import com.stm.pegelhub.lib.model.*;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.HttpEntities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

public class HttpPegelHubCommunicator implements PegelHubCommunicator {

    private static final Logger LOG = LoggerFactory.getLogger(HttpPegelHubCommunicator.class);
    private final String measurementRoute;
    private final String telemetryRoute;
    private final String contactRoute;
    private final String connectorRoute;
    private final String tokenRoute;
    private final String takerRoute;
    private final String supplierRoute;
    private final URL baseUrl;
    private final CloseableHttpClient client;
    private final ApplicationProperties properties;

    private String routeWithApiKey(String route) {
        return route + (route.contains("?") ? "&" : "?") + "apiKey=" + properties.getApiKey();
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
            final URI uri = baseUrl.toURI().resolve(routeWithApiKey(supplierRoute));
            final var http = new HttpPost(uri);
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
            final URI uri = baseUrl.toURI().resolve(routeWithApiKey(takerRoute));
            final var http = new HttpPost(uri);
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

    private void refreshApiKey() {
        try {
            Collection<Supplier> suppliers = getSuppliers();
            SupplierSendDto supProperties = properties.getSupplier();
            String supplierNumber = supProperties.stationNumber();
            UUID supplierID = null;

            Set<Supplier> supplierSet = suppliers.stream().filter(s -> supplierNumber.equals(s.getStationNumber())).collect(Collectors.toSet());
            Optional<Supplier> optSupplier = null;
            Supplier supplier = null;
            if(!supplierSet.isEmpty()) {
                optSupplier = supplierSet.stream().findFirst();
                supplier = optSupplier.get();
                supplierID = UUID.fromString(supplier.getId());
            }

            assert supplierID != null;
            UUID connectorUUID = getConnectorID(supplierID);
            LOG.debug("connectorUUID: " + connectorUUID);
            final URI uri = baseUrl.toURI().resolve(routeWithApiKey(tokenRoute) + "&uuid=" + connectorUUID.toString());
            final var http = new HttpPut(uri);

            LOG.debug(uri.toString());

            Optional<ApiToken> dto = client.execute(http, response -> {
                if (response.getCode() != HttpStatus.SC_OK) {
                    throw new RuntimeException("Invalid status: " + response.getCode());
                }
                var json = EntityUtils.toString(response.getEntity());
                var gson = new Gson();
                var mapType = new TypeToken<ApiTokenReceiveDto>() {
                };
                var token = gson.fromJson(json, mapType);
                if (token == null) {
                    return Optional.empty();
                }
                return Optional.of(token.toApiToken());
            });
            dto.ifPresent(token -> properties.setApiKey(token.getApiKey()));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public HttpPegelHubCommunicator(CloseableHttpClient client, URL baseUrl, ApplicationProperties properties) {
        this(client, baseUrl, properties,
                "api/v1/measurement/",
                "api/v1/telemetry/",
                "api/v1/contact/",
                "api/v1/connector/",
                "api/v1/token",
                "api/v1/taker",
                "api/v1/supplier");
    }

    public HttpPegelHubCommunicator(CloseableHttpClient client, URL baseUrl, ApplicationProperties properties, String measurementRoute, String telemetryRoute, String contactRoute, String connectorRoute, String tokenRoute, String takerRoute, String supplierRoute) {
        this.client = client;
        this.baseUrl = baseUrl;
        this.measurementRoute = measurementRoute;
        this.telemetryRoute = telemetryRoute;
        this.contactRoute = contactRoute;
        this.connectorRoute = connectorRoute;
        this.tokenRoute = tokenRoute;
        this.takerRoute = takerRoute;
        this.supplierRoute = supplierRoute;
        this.properties = properties;
        if (properties.isRefreshNecessary()) {
            refreshApiKey();
        }
        if (properties.isSupplierDataToSend()) {
            sendMetaData();
        }
    }

    @Override
    public Collection<Measurement> getMeasurements(String timespan) {
        try {
            ensureIsTaker();

            final URI uri = baseUrl.toURI().resolve(routeWithApiKey(measurementRoute + timespan));
            final var http = new HttpGet(uri);

            return client.execute(http, response -> {
                var json = EntityUtils.toString(response.getEntity());
                var gson = new GsonBuilder()
                        .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeConverter())
                        .create();
                var listType = new TypeToken<List<Measurement>>() {
                };
                List<Measurement> meass = gson.fromJson(json, listType);

                for(Measurement m : meass)
                {
                    if(m.getTimestamp() == null) {
                        String timeStampWithOffset = m.getInfos().get("TimestampWithOffset");
                        OffsetDateTime offsetDateTime = OffsetDateTime.parse(timeStampWithOffset);
                        m.setTimestamp(convertedTime(m.getTimestamp(), offsetDateTime.getOffset()));
                    }
                }

                return gson.fromJson(json, listType);
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Measurement> getMeasurementByUUID(UUID uuid) {
        try {
            ensureIsTaker();

            final URI uri = baseUrl.toURI().resolve(routeWithApiKey(measurementRoute + "last/" + uuid.toString()));
            final var http = new HttpGet(uri);

            return Optional.ofNullable(client.execute(http, response -> {
                if (response.getCode() != HttpStatus.SC_OK) {
                    throw new RuntimeException("Invalid status: " + response.getCode());
                }

                var json = EntityUtils.toString(response.getEntity());
                var gson = new GsonBuilder()
                        .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeConverter())
                        .create();
                return gson.fromJson(json, Measurement.class);
            }));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Collection<Measurement> getMeasurementsOfStation(String stationNumber, String timespan) {
        ensureIsTaker();
        return _getMeasurementsByStationAndTime(stationNumber, timespan);
    }

    @Override
    public Optional<Measurement> getLatestMeasurementOfStation() {
        ensureIsTaker();
        return _getLatestMeasurementByStation(properties.getTaker().stationNumber());
    }

    @Override
    public Collection<Telemetry> getTelemetry(String timespan) {
        try {
            ensureIsTaker();
            final URI uri = baseUrl.toURI().resolve(routeWithApiKey(telemetryRoute + timespan));
            final var http = new HttpGet(uri);
            LOG.debug("Executing GET request to URI: {}", uri);

            return client.execute(http, response -> {
                var json = EntityUtils.toString(response.getEntity());
                var gson = new Gson();
                var mapType = new TypeToken<Map<String, Map<String, TelemetryCollectionReceiveDto.TelemetryReceiveDtoInnerType>>>() {
                };

                Collection<Telemetry> telCol = new TelemetryCollectionReceiveDto(gson.fromJson(json, mapType)).toTelemetryCollection();
                List<Telemetry> telList = telCol.stream().toList();
                for(Telemetry t : telList)
                {
                    t.setTimestamp(convertedTime(t.getTimestamp(), null));
                }
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
            final URI uri = baseUrl.toURI().resolve(routeWithApiKey(telemetryRoute + "last/" + uuid.toString()));
            final var http = new HttpGet(uri);

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
                if(returnValue.isPresent()) {
                    returnValue.get().setTimestamp(convertedTime(returnValue.get().getTimestamp(), null));
                }
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

            for(Measurement m : meass)
            {
                if(m.getTimestamp() == null) {
                    String timeStampWithOffset = m.getInfos().get("TimestampWithOffset");
                    OffsetDateTime offsetDateTime = OffsetDateTime.parse(timeStampWithOffset);
                    m.setTimestamp(convertedTime(m.getTimestamp(),offsetDateTime.getOffset()));
                    Thread.sleep(500);
                }
            }

            final URI uri = baseUrl.toURI().resolve(routeWithApiKey(measurementRoute));
            final var http = new HttpPost(uri);
            http.setHeader("Content-Type", "application/json");
            var dto = new MeasurementsSendDto(meass.stream().map(m -> new MeasurementSendDto(m.getTimestamp(), m.getFields(), m.getInfos())).toList());
            var gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeConverter())
                    .create();
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

    @Override
    public void sendTelemetry(Telemetry tel) {
        try {
            ensureIsSupplier();

            tel.setTimestamp(convertedTime(tel.getTimestamp(), null));

            final URI uri = baseUrl.toURI().resolve(routeWithApiKey(telemetryRoute));
            final var http = new HttpPost(uri);
            http.setHeader("Content-Type", "application/json");

            var gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeConverter())
                    .create();
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

            final URI uri = baseUrl.toURI().resolve(routeWithApiKey(connectorRoute));
            final var http = new HttpGet(uri);

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

            final URI uri = baseUrl.toURI().resolve(routeWithApiKey(connectorRoute + uuid.toString()));
            final var http = new HttpGet(uri);

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

            final URI uri = baseUrl.toURI().resolve(routeWithApiKey(connectorRoute));
            final var http = new HttpPost(uri);
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
            final URI uri = baseUrl.toURI().resolve(routeWithApiKey(contactRoute));
            final var http = new HttpGet(uri);

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
            final URI uri = baseUrl.toURI().resolve(routeWithApiKey(contactRoute + uuid.toString()));
            final var http = new HttpGet(uri);

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
            final URI uri = baseUrl.toURI().resolve(routeWithApiKey(connectorRoute));
            final var http = new HttpPost(uri);
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

    public Timestamp getSystemTime() {
        try {
            final URI uri = baseUrl.toURI().resolve(measurementRoute + "//systemTime");
            final var http = new HttpGet(uri);
            return client.execute(http, response -> {
                var json = EntityUtils.toString(response.getEntity());
                var gson = new Gson();
                var mapType = new TypeToken<Timestamp>() {
                };
                return gson.fromJson(json, mapType);
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
            final URI uri = baseUrl.toURI().resolve(supplierRoute + "//connectorID//" + uuid.toString());
            LOG.debug(uri + "");
            final var http = new HttpGet(uri);
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

    //TODO: Verwend ich das überhaupt irgendwo? Ist der gleiche Code wie getMeasurementByUUID.
    public Optional<Measurement> getTimestampOfLastMeasurementByUUID(UUID uuid) {
        try {

            final URI uri = baseUrl.toURI().resolve(routeWithApiKey(measurementRoute + "last/" + uuid.toString()));
            final var http = new HttpGet(uri);

            return Optional.ofNullable(client.execute(http, response -> {
                if (response.getCode() != HttpStatus.SC_OK) {
                    throw new RuntimeException("Invalid status: " + response.getCode());
                }

                var json = EntityUtils.toString(response.getEntity());
                var gson = new GsonBuilder()
                        .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeConverter())
                        .create();
                return gson.fromJson(json, Measurement.class);
            }));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public HashSet<Long> getMeasurementsIDsOfStation(String stationNumber, String timespan) {

        Collection<Measurement> measurements = _getMeasurementsByStationAndTime(stationNumber, timespan);
        HashSet<Long> IDs = new HashSet<>();

        for(Measurement m : measurements)
        {
            //TODO needs to be changed at some point - but first need to find out why ID is needed
            if(m.getFields().containsKey("ID")){
                Long work = Math.round(m.getFields().get("ID"));
                IDs.add(work);
            }
        }
        return IDs;
    }

    private Collection<Measurement> _getMeasurementsByStationAndTime(String stationNumber, String timespan) {
        try {
            final URI uri = baseUrl.toURI().resolve(routeWithApiKey(measurementRoute + "supplier/" + timespan + "?stationNumber=" + stationNumber));
            final var http = new HttpGet(uri);

            return client.execute(http, response -> {
                if (response.getCode() == 404) {
                    throw new NotFoundException("supplier does not exist");
                }
                var json = EntityUtils.toString(response.getEntity());
                var gson = new GsonBuilder()
                        .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeConverter())
                        .create();
                var listType = new TypeToken<List<Measurement>>() {
                };
                return gson.fromJson(json, listType);
            });
        } catch (NotFoundException nfe) {
            throw new NotFoundException(nfe.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private Optional<Measurement> _getLatestMeasurementByStation(String stationNumber) {
        try {
            ensureIsTaker();

            final URI uri = baseUrl.toURI()
                    .resolve(routeWithApiKey(measurementRoute + "supplier/latest?stationNumber=" + stationNumber));
            final var http = new HttpGet(uri);

            return Optional.ofNullable(client.execute(http, response -> {
                if (response.getCode() != HttpStatus.SC_OK) {
                    return null;
                }

                var json = EntityUtils.toString(response.getEntity());
                var gson = new GsonBuilder()
                        .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeConverter())
                        .create();
                return gson.fromJson(json, Measurement.class);
            }));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private LocalDateTime convertedTime(LocalDateTime pTime, ZoneOffset offset) {
        if (offset == null) {
            if (!properties.isSupplier()) {
                LOG.debug("Timestamp before converting: " + pTime);
                ZonedDateTime originalDateTime = pTime.atZone(ZoneId.of("UTC"));
                ZonedDateTime convertedDateTime = ZonedDateTime.ofInstant(
                        originalDateTime.toInstant(),
                        ZoneOffset.systemDefault());
                LOG.debug("Timestamp after converting: " + convertedDateTime);
                return convertedDateTime.toLocalDateTime();
            } else {
                LOG.debug("Timestamp before converting: " + pTime);
                ZonedDateTime originalDateTime = pTime.atZone(ZoneId.systemDefault());
                ZonedDateTime convertedDateTime = ZonedDateTime.ofInstant(
                        originalDateTime.toInstant(),
                        ZoneOffset.UTC);
                LOG.debug("Timestamp after converting: " + convertedDateTime);
                return convertedDateTime.toLocalDateTime();
            }
        } else {
            if (!properties.isSupplier()) {
                LOG.debug("Timestamp before converting: " + pTime);
                ZonedDateTime originalDateTime = pTime.atZone(ZoneId.of("UTC"));
                ZonedDateTime convertedDateTime = ZonedDateTime.ofInstant(
                        originalDateTime.toInstant(),
                        offset);
                LOG.debug("Timestamp after converting: " + convertedDateTime);
                return convertedDateTime.toLocalDateTime();
            } else {
                LOG.debug("Timestamp before converting: " + pTime);
                ZonedDateTime originalDateTime = pTime.atZone(offset);
                ZonedDateTime convertedDateTime = ZonedDateTime.ofInstant(
                        originalDateTime.toInstant(),
                        ZoneOffset.UTC);
                LOG.debug("Timestamp after converting: " + convertedDateTime);
                return convertedDateTime.toLocalDateTime();
            }
        }
    }
}

