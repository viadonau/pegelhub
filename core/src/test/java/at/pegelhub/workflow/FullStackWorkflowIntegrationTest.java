package at.pegelhub.workflow;

import at.pegelhub.connector.api.CreateConnectorDto;
import at.pegelhub.connector.api.RegisterConnectorRequest;
import at.pegelhub.connector.domain.ConnectorStatus;
import at.pegelhub.contact.api.CreateContactDto;
import at.pegelhub.measurement.api.WriteMeasurementDto;
import at.pegelhub.measurement.api.WriteMeasurementsDto;
import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.supplier.api.CreateStationManufacturerDto;
import at.pegelhub.supplier.api.CreateSupplierDto;
import at.pegelhub.supplier.api.SupplierDto;
import at.pegelhub.taker.api.CreateTakerDto;
import at.pegelhub.taker.api.CreateTakerServiceManufacturerDto;
import at.pegelhub.taker.api.TakerDto;
import at.pegelhub.telemetry.domain.Telemetry;
import at.pegelhub.testsupport.FullStackIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
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
    void supplierMeasurementWorkflowWritesAndReadsInfluxDataThroughHttp() {
        String suffix = compactId(UUID.randomUUID());
        AuthToken operator = token("operator-" + suffix, "local-operator", List.of("metadata:write", "system:admin"));
        AuthToken connectorToken = token(
                "measurement-" + suffix,
                "supplier-client-" + suffix,
                List.of("measurement:write", "measurement:read"));
        String stationNumber = "supplier-station-" + suffix;
        SupplierDto supplier = postSupplier(operator, connectorToken, stationNumber);
        Instant latestTimestamp = Instant.now().minus(5, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS);
        WriteMeasurementsDto measurements = new WriteMeasurementsDto(List.of(
                new WriteMeasurementDto(
                        latestTimestamp.minus(4, ChronoUnit.HOURS),
                        Map.of("waterLevel", 10.5, "flow", 20.5),
                        Map.of("quality", "old")),
                new WriteMeasurementDto(
                        latestTimestamp,
                        Map.of("waterLevel", 11.5, "flow", 21.5),
                        Map.of("quality", "latest"))));

        ResponseEntity<Void> writeResponse = rest.exchange(
                "/api/v1/measurement",
                HttpMethod.POST,
                bearerEntity(measurements, connectorToken),
                Void.class);
        ResponseEntity<Measurement> latestResponse = rest.exchange(
                "/api/v1/measurement/supplier/latest?stationNumber={stationNumber}",
                HttpMethod.GET,
                bearerEntity(null, connectorToken),
                Measurement.class,
                stationNumber);
        ResponseEntity<List<Measurement>> rangeResponse = rest.exchange(
                "/api/v1/measurement/supplier/3h?stationNumber={stationNumber}",
                HttpMethod.GET,
                bearerEntity(null, connectorToken),
                new ParameterizedTypeReference<>() {
                },
                stationNumber);
        ResponseEntity<Measurement> averageResponse = rest.exchange(
                "/api/v1/measurement/supplier/average/6h?stationNumber={stationNumber}",
                HttpMethod.GET,
                bearerEntity(null, connectorToken),
                Measurement.class,
                stationNumber);
        ResponseEntity<String> systemTimeResponse = rest.getForEntity(
                "/api/v1/measurement/systemTime",
                String.class);
        ResponseEntity<String> missingSupplierResponse = rest.exchange(
                "/api/v1/measurement/supplier/latest?stationNumber={stationNumber}",
                HttpMethod.GET,
                bearerEntity(null, connectorToken),
                String.class,
                "missing-supplier");
        ResponseEntity<String> invalidRangeResponse = rest.exchange(
                "/api/v1/measurement/supplier/not-a-range?stationNumber={stationNumber}",
                HttpMethod.GET,
                bearerEntity(null, connectorToken),
                String.class,
                stationNumber);

        assertThat(writeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(latestResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertMeasurement(latestResponse.getBody(), supplier.id(), latestTimestamp, "latest", 11.5, 21.5);
        assertThat(rangeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rangeResponse.getBody()).singleElement()
                .satisfies(measurement -> assertMeasurement(measurement, supplier.id(), latestTimestamp, "latest", 11.5, 21.5));
        assertThat(averageResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(averageResponse.getBody()).isNotNull();
        assertThat(averageResponse.getBody().measurement()).isEqualTo(supplier.id());
        assertThat(averageResponse.getBody().fields()).containsKeys("waterLevel", "flow");
        assertThat(systemTimeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(systemTimeResponse.getBody()).contains("T");
        assertThat(missingSupplierResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(invalidRangeResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void takerTelemetryWorkflowWritesAndReadsInfluxDataThroughHttp() {
        String suffix = compactId(UUID.randomUUID());
        AuthToken operator = token("operator-" + suffix, "local-operator", List.of("metadata:write", "system:admin"));
        AuthToken connectorToken = token(
                "telemetry-" + suffix,
                "taker-client-" + suffix,
                List.of("telemetry:write", "telemetry:read"));
        TakerDto taker = postTaker(operator, connectorToken, "taker-station-" + suffix);
        Instant timestamp = Instant.now().minus(5, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS);
        Telemetry telemetry = new Telemetry(
                taker.id().toString(),
                "10.0.0.5",
                "203.0.113.5",
                timestamp,
                30,
                7.5,
                8.5,
                12.5,
                24.5,
                1.5,
                2.5,
                90.5);

        ResponseEntity<Telemetry> writeResponse = rest.exchange(
                "/api/v1/telemetry",
                HttpMethod.POST,
                bearerEntity(telemetry, connectorToken),
                Telemetry.class);
        ResponseEntity<Telemetry> latestResponse = rest.exchange(
                "/api/v1/telemetry/last/{uuid}",
                HttpMethod.GET,
                bearerEntity(null, connectorToken),
                Telemetry.class,
                taker.id());
        ResponseEntity<List<Telemetry>> rangeResponse = rest.exchange(
                "/api/v1/telemetry/1h",
                HttpMethod.GET,
                bearerEntity(null, connectorToken),
                new ParameterizedTypeReference<>() {
                });

        assertThat(writeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(writeResponse.getBody()).isEqualTo(telemetry);
        assertThat(latestResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(latestResponse.getBody()).isEqualTo(telemetry);
        assertThat(rangeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rangeResponse.getBody())
                .isNotNull()
                .filteredOn(entry -> telemetry.measurement().equals(entry.measurement()))
                .contains(telemetry);
    }

    private SupplierDto postSupplier(AuthToken operator, AuthToken connectorToken, String stationNumber) {
        String connectorNumber = "sc-" + compactId(connectorToken.clientId());
        registerConnector(operator, connectorToken.clientId(), connectorNumber);

        ResponseEntity<SupplierDto> response = rest.exchange(
                "/api/v1/supplier",
                HttpMethod.POST,
                bearerEntity(supplierDto(stationNumber, connectorNumber), operator),
                SupplierDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private TakerDto postTaker(AuthToken operator, AuthToken connectorToken, String stationNumber) {
        String connectorNumber = "tc-" + compactId(connectorToken.clientId());
        registerConnector(operator, connectorToken.clientId(), connectorNumber);

        ResponseEntity<TakerDto> response = rest.exchange(
                "/api/v1/taker",
                HttpMethod.POST,
                bearerEntity(takerDto(stationNumber, connectorNumber), operator),
                TakerDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private void registerConnector(AuthToken operator, String keycloakClientId, String connectorNumber) {
        RegisterConnectorRequest request = new RegisterConnectorRequest(
                keycloakClientId,
                ConnectorStatus.ACTIVE,
                connectorDto(connectorNumber));

        ResponseEntity<Void> response = rest.exchange(
                "/api/v1/admin/connectors",
                HttpMethod.POST,
                bearerEntity(request, operator),
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
            Measurement measurement,
            UUID expectedId,
            Instant expectedTimestamp,
            String expectedQuality,
            double expectedWaterLevel,
            double expectedFlow) {
        assertThat(measurement).isNotNull();
        assertThat(measurement.measurement()).isEqualTo(expectedId);
        assertThat(measurement.timestamp()).isEqualTo(expectedTimestamp);
        assertThat(measurement.infos()).containsEntry("quality", expectedQuality);
        assertThat(measurement.fields()).containsEntry("waterLevel", expectedWaterLevel);
        assertThat(measurement.fields()).containsEntry("flow", expectedFlow);
    }

    private static CreateSupplierDto supplierDto(String stationNumber, String connectorNumber) {
        return new CreateSupplierDto(
                stationNumber,
                101,
                "supplier station",
                "Danube",
                'W',
                new CreateStationManufacturerDto("station manufacturer", "station type", "1.0.0", "station remark"),
                connectorDto(connectorNumber),
                300_000L,
                0.5,
                "main usage",
                "normal",
                1.0,
                "reference place",
                2.0,
                "left",
                48.1,
                16.2,
                48.3,
                16.4,
                3.0,
                4.0,
                5,
                6.0,
                7,
                8.0,
                9,
                10.0,
                11.0,
                12.0,
                13.0,
                "channel",
                false,
                false);
    }

    private static CreateTakerDto takerDto(String stationNumber, String connectorNumber) {
        return new CreateTakerDto(
                stationNumber,
                201,
                new CreateTakerServiceManufacturerDto("taker manufacturer", "taker system", "1.0.0", "request remark"),
                connectorDto(connectorNumber),
                600_000L);
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
