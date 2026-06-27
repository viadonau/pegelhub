package at.pegelhub.workflow;

import at.pegelhub.access.api.CreateAccessGrantRequest;
import at.pegelhub.access.domain.AccessPermission;
import at.pegelhub.access.domain.AccessResourceType;
import at.pegelhub.connector.api.ConnectorDto;
import at.pegelhub.connector.api.CreateConnectorDto;
import at.pegelhub.connector.api.RegisterConnectorRequest;
import at.pegelhub.connector.domain.ConnectorStatus;
import at.pegelhub.contact.api.CreateContactDto;
import at.pegelhub.measurement.api.read.output.MeasurementListResponse;
import at.pegelhub.measurement.api.read.output.MeasurementPointResponse;
import at.pegelhub.measurement.api.write.WriteMeasurementRequest;
import at.pegelhub.measurement.api.write.WriteMeasurementsRequest;
import at.pegelhub.station.api.CreateStationRequest;
import at.pegelhub.station.api.StationResponse;
import at.pegelhub.stationowner.api.CreateStationOwnerRequest;
import at.pegelhub.stationowner.api.StationOwnerResponse;
import at.pegelhub.testsupport.FullStackIntegrationTestBase;
import at.pegelhub.timeseries.api.CreateTimeSeriesRequest;
import at.pegelhub.timeseries.api.TimeSeriesResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

final class FullStackWorkflowIntegrationTest extends FullStackIntegrationTestBase {

    private static final String ISSUER = "http://localhost:8082/realms/pegelhub";

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void timeSeriesMeasurementWorkflowWritesAndReadsInfluxDataThroughHttp() {
        String suffix = compactId(UUID.randomUUID());
        AuthToken operator = token("operator-" + suffix, "local-operator", List.of("metadata:write", "system:admin"));
        AuthToken connectorToken = token(
                "measurement-" + suffix,
                "supplier-client-" + suffix,
                List.of("measurement:write", "measurement:read"));
        ConnectorDto connector = registerConnector(operator, connectorToken.clientId(), "mc-" + compactId(connectorToken.clientId()));
        StationOwnerResponse owner = postStationOwner(operator, "Owner " + suffix);
        StationResponse station = postStation(operator, owner.id(), "station-" + suffix);
        TimeSeriesResponse timeSeries = postTimeSeries(operator, station.id(), "water-level", "cm");
        grantWriteAccess(operator, connector.id(), timeSeries.id());
        grantReadAccess(operator, connector.id(), timeSeries.id());
        Instant latestTimestamp = Instant.now().minus(5, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS);
        WriteMeasurementsRequest measurements = new WriteMeasurementsRequest(List.of(
                new WriteMeasurementRequest(
                        timeSeries.id(),
                        latestTimestamp.minus(4, ChronoUnit.HOURS),
                        10.5),
                new WriteMeasurementRequest(
                        timeSeries.id(),
                        latestTimestamp,
                        11.5)));

        ResponseEntity<Void> writeResponse = rest.exchange(
                "/api/v1/measurements",
                HttpMethod.POST,
                bearerEntity(measurements, connectorToken),
                Void.class);
        ResponseEntity<MeasurementListResponse> latestResponse = rest.exchange(
                "/api/v1/time-series/{timeSeriesId}/measurements?last=3h&order=desc&limit=1",
                HttpMethod.GET,
                bearerEntity(null, connectorToken),
                MeasurementListResponse.class,
                timeSeries.id());
        ResponseEntity<MeasurementListResponse> rangeResponse = rest.exchange(
                "/api/v1/time-series/{timeSeriesId}/measurements?last=3h",
                HttpMethod.GET,
                bearerEntity(null, connectorToken),
                MeasurementListResponse.class,
                timeSeries.id());
        ResponseEntity<String> systemTimeResponse = rest.getForEntity(
                "/api/v1/measurements/system-time",
                String.class);

        assertThat(writeResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(latestResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(latestResponse.getBody()).isNotNull();
        assertThat(latestResponse.getBody().timeSeriesId()).isEqualTo(timeSeries.id());
        assertThat(latestResponse.getBody().measurements())
                .singleElement()
                .satisfies(measurement -> assertMeasurement(measurement, latestTimestamp, 11.5));
        assertThat(rangeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rangeResponse.getBody()).isNotNull();
        assertThat(rangeResponse.getBody().timeSeriesId()).isEqualTo(timeSeries.id());
        assertThat(rangeResponse.getBody().measurements())
                .singleElement()
                .satisfies(measurement -> assertMeasurement(measurement, latestTimestamp, 11.5));
        assertThat(systemTimeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(systemTimeResponse.getBody()).contains("T");
    }

    private ConnectorDto registerConnector(AuthToken operator, String keycloakClientId, String connectorNumber) {
        RegisterConnectorRequest request = new RegisterConnectorRequest(
                keycloakClientId,
                ConnectorStatus.ACTIVE,
                connectorDto(connectorNumber));

        ResponseEntity<ConnectorDto> response = rest.exchange(
                "/api/v1/admin/connectors",
                HttpMethod.POST,
                bearerEntity(request, operator),
                ConnectorDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private StationOwnerResponse postStationOwner(AuthToken operator, String name) {
        ResponseEntity<StationOwnerResponse> response = rest.exchange(
                "/api/v1/station-owners",
                HttpMethod.POST,
                bearerEntity(new CreateStationOwnerRequest(name, compactId(name), null), operator),
                StationOwnerResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private StationResponse postStation(AuthToken operator, UUID ownerId, String stationNumber) {
        ResponseEntity<StationResponse> response = rest.exchange(
                "/api/v1/stations",
                HttpMethod.POST,
                bearerEntity(new CreateStationRequest(ownerId, stationNumber, "Station " + stationNumber, "Danube", "Wachau"), operator),
                StationResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private TimeSeriesResponse postTimeSeries(AuthToken operator, UUID stationId, String observedProperty, String unit) {
        ResponseEntity<TimeSeriesResponse> response = rest.exchange(
                "/api/v1/time-series",
                HttpMethod.POST,
                bearerEntity(new CreateTimeSeriesRequest(stationId, observedProperty, unit, null, null, null), operator),
                TimeSeriesResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private void grantWriteAccess(AuthToken operator, UUID connectorId, UUID timeSeriesId) {
        grantAccess(operator, connectorId, timeSeriesId, AccessPermission.WRITE);
    }

    private void grantReadAccess(AuthToken operator, UUID connectorId, UUID timeSeriesId) {
        grantAccess(operator, connectorId, timeSeriesId, AccessPermission.READ);
    }

    private void grantAccess(AuthToken operator, UUID connectorId, UUID timeSeriesId, AccessPermission permission) {
        ResponseEntity<Void> response = rest.exchange(
                "/api/v1/access-grants",
                HttpMethod.POST,
                bearerEntity(new CreateAccessGrantRequest(
                        connectorId,
                        AccessResourceType.TIME_SERIES,
                        timeSeriesId,
                        permission), operator),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private AuthToken token(String tokenValue, String clientId, List<String> roles) {
        when(jwtDecoder.decode(tokenValue)).thenReturn(Jwt.withTokenValue(tokenValue)
                .header("alg", "none")
                .issuer(ISSUER)
                .subject("subject")
                .audience(List.of("pegelhub-core-api"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .claim("azp", clientId)
                .claim("resource_access", Map.of("pegelhub-core-api", Map.of("roles", roles)))
                .build());
        return new AuthToken(tokenValue, clientId);
    }

    private static HttpEntity<?> bearerEntity(Object body, AuthToken token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.value());
        return new HttpEntity<>(body, headers);
    }

    private static void assertMeasurement(
            MeasurementPointResponse measurement,
            Instant expectedTimestamp,
            double expectedValue) {
        assertThat(measurement).isNotNull();
        assertThat(measurement.observedAt()).isEqualTo(expectedTimestamp);
        assertThat(measurement.value()).isEqualTo(expectedValue);
    }

    private static CreateConnectorDto connectorDto(String connectorNumber) {
        CreateContactDto contact = contactDto();
        return new CreateConnectorDto(
                connectorNumber,
                contact,
                "type",
                "1.0.0",
                "1.0.0",
                "definition",
                contact,
                contact,
                contact,
                "notes");
    }

    private static String compactId(UUID id) {
        return id.toString().substring(0, 8);
    }

    private static String compactId(String value) {
        return value.length() <= 8 ? value : value.substring(value.length() - 8);
    }

    private static CreateContactDto contactDto() {
        return new CreateContactDto(
                "organization",
                "contact person",
                "street",
                "1234",
                "Vienna",
                "AT",
                "111",
                "112",
                "emergency@example.org",
                "211",
                "212",
                "service@example.org",
                "311",
                "312",
                "admin@example.org",
                "notes");
    }

    private record AuthToken(String value, String clientId) {
    }
}
