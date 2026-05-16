package at.pegelhub.workflow;

import at.pegelhub.auth.api.ApiTokenDto;
import at.pegelhub.connector.api.CreateConnectorDto;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

final class FullStackWorkflowIntegrationTest extends FullStackIntegrationTestBase {

    @Test
    void tokenWorkflowRejectsInvalidApiKeysAndAcceptsActivatedToken() {
        ActivatedToken token = createActivatedToken();

        ResponseEntity<String> invalidResponse = rest.getForEntity(
                "/api/v1/measurement/1h?apiKey={apiKey}",
                String.class,
                "invalid-key");
        ResponseEntity<String> validResponse = rest.getForEntity(
                "/api/v1/measurement/1h?apiKey={apiKey}",
                String.class,
                token.apiKey());

        assertThat(invalidResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(validResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void tokenRefreshAndInvalidateLifecycleWorksOverHttp() {
        ActivatedToken token = createActivatedToken();
        SupplierDto supplier = postSupplier(token, "supplier-token-" + compactId(token.id()));
        UUID connectorId = supplier.connector().id();

        ResponseEntity<ApiTokenDto> refreshResponse = rest.exchange(
                "/api/v1/token?apiKey={apiKey}&uuid={uuid}",
                HttpMethod.PUT,
                null,
                ApiTokenDto.class,
                token.apiKey(),
                connectorId);
        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshResponse.getBody()).isNotNull();
        String refreshedApiKey = refreshResponse.getBody().apiKey();

        ResponseEntity<String> oldTokenAfterRefresh = rest.getForEntity(
                "/api/v1/measurement/1h?apiKey={apiKey}",
                String.class,
                token.apiKey());
        ResponseEntity<String> refreshedTokenAccess = rest.getForEntity(
                "/api/v1/measurement/1h?apiKey={apiKey}",
                String.class,
                refreshedApiKey);

        ResponseEntity<Void> invalidateResponse = rest.exchange(
                "/api/v1/token?apiKey={apiKey}&uuid={uuid}",
                HttpMethod.DELETE,
                null,
                Void.class,
                refreshedApiKey,
                connectorId);
        ResponseEntity<String> refreshedTokenAfterInvalidate = rest.getForEntity(
                "/api/v1/measurement/1h?apiKey={apiKey}",
                String.class,
                refreshedApiKey);

        assertThat(refreshedApiKey).isNotEqualTo(token.apiKey());
        assertThat(oldTokenAfterRefresh.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(refreshedTokenAccess.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(invalidateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshedTokenAfterInvalidate.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void supplierMeasurementWorkflowWritesAndReadsInfluxDataThroughHttp() {
        ActivatedToken token = createActivatedToken();
        String stationNumber = "supplier-station-" + compactId(token.id());
        SupplierDto supplier = postSupplier(token, stationNumber);
        LocalDateTime latestTimestamp = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(5).truncatedTo(ChronoUnit.SECONDS);
        WriteMeasurementsDto measurements = new WriteMeasurementsDto(List.of(
                new WriteMeasurementDto(
                        latestTimestamp.minusHours(4),
                        Map.of("waterLevel", 10.5, "flow", 20.5),
                        Map.of("quality", "old")),
                new WriteMeasurementDto(
                        latestTimestamp,
                        Map.of("waterLevel", 11.5, "flow", 21.5),
                        Map.of("quality", "latest"))));

        ResponseEntity<Void> writeResponse = rest.postForEntity(
                "/api/v1/measurement?apiKey={apiKey}",
                measurements,
                Void.class,
                token.apiKey());
        ResponseEntity<Measurement> latestResponse = rest.getForEntity(
                "/api/v1/measurement/supplier/latest?apiKey={apiKey}&stationNumber={stationNumber}",
                Measurement.class,
                token.apiKey(),
                stationNumber);
        ResponseEntity<List<Measurement>> rangeResponse = rest.exchange(
                "/api/v1/measurement/supplier/3h?apiKey={apiKey}&stationNumber={stationNumber}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                },
                token.apiKey(),
                stationNumber);
        ResponseEntity<Measurement> averageResponse = rest.getForEntity(
                "/api/v1/measurement/supplier/average/6h?apiKey={apiKey}&stationNumber={stationNumber}",
                Measurement.class,
                token.apiKey(),
                stationNumber);
        ResponseEntity<String> systemTimeResponse = rest.getForEntity(
                "/api/v1/measurement/systemTime",
                String.class);
        ResponseEntity<String> missingSupplierResponse = rest.getForEntity(
                "/api/v1/measurement/supplier/latest?apiKey={apiKey}&stationNumber={stationNumber}",
                String.class,
                token.apiKey(),
                "missing-supplier");
        ResponseEntity<String> invalidRangeResponse = rest.getForEntity(
                "/api/v1/measurement/supplier/not-a-range?apiKey={apiKey}&stationNumber={stationNumber}",
                String.class,
                token.apiKey(),
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
        ActivatedToken token = createActivatedToken();
        TakerDto taker = postTaker(token, "taker-station-" + compactId(token.id()));
        String timestamp = Instant.now().minus(5, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS).toString();
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

        ResponseEntity<Telemetry> writeResponse = rest.postForEntity(
                "/api/v1/telemetry?apiKey={apiKey}",
                telemetry,
                Telemetry.class,
                token.apiKey());
        ResponseEntity<Telemetry> latestResponse = rest.getForEntity(
                "/api/v1/telemetry/last/{uuid}?apiKey={apiKey}",
                Telemetry.class,
                taker.id(),
                token.apiKey());
        ResponseEntity<List<Telemetry>> rangeResponse = rest.exchange(
                "/api/v1/telemetry/1h?apiKey={apiKey}",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                },
                token.apiKey());

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

    private ActivatedToken createActivatedToken() {
        Set<UUID> before = getTokenIds();

        ResponseEntity<ApiTokenDto> created = rest.postForEntity("/api/v1/token", null, ApiTokenDto.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(created.getBody()).isNotNull();

        Set<UUID> after = getTokenIds();
        after.removeAll(before);
        assertThat(after).hasSize(1);
        UUID id = after.iterator().next();

        rest.put("/api/v1/token/admin?uuid={uuid}", null, id);
        return new ActivatedToken(id, created.getBody().apiKey());
    }

    private Set<UUID> getTokenIds() {
        ResponseEntity<UUID[]> response = rest.getForEntity("/api/v1/token/admin", UUID[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        UUID[] body = response.getBody();
        if (body == null) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(body));
    }

    private SupplierDto postSupplier(ActivatedToken token, String stationNumber) {
        ResponseEntity<SupplierDto> response = rest.postForEntity(
                "/api/v1/supplier?apiKey={apiKey}",
                supplierDto(stationNumber, "sc-" + compactId(token.id()), token.id()),
                SupplierDto.class,
                token.apiKey());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private TakerDto postTaker(ActivatedToken token, String stationNumber) {
        ResponseEntity<TakerDto> response = rest.postForEntity(
                "/api/v1/taker?apiKey={apiKey}",
                takerDto(stationNumber, "tc-" + compactId(token.id()), token.id()),
                TakerDto.class,
                token.apiKey());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private static void assertMeasurement(
            Measurement measurement,
            UUID expectedId,
            LocalDateTime expectedTimestamp,
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

    private static CreateSupplierDto supplierDto(String stationNumber, String connectorNumber, UUID apiToken) {
        return new CreateSupplierDto(
                stationNumber,
                101,
                "supplier station",
                "Danube",
                'W',
                new CreateStationManufacturerDto("station manufacturer", "station type", "1.0.0", "station remark"),
                connectorDto(connectorNumber, apiToken),
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

    private static CreateTakerDto takerDto(String stationNumber, String connectorNumber, UUID apiToken) {
        return new CreateTakerDto(
                stationNumber,
                201,
                new CreateTakerServiceManufacturerDto("taker manufacturer", "taker system", "1.0.0", "request remark"),
                connectorDto(connectorNumber, apiToken),
                600_000L);
    }

    private static CreateConnectorDto connectorDto(String connectorNumber, UUID apiToken) {
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
                "notes",
                apiToken);
    }

    private static String compactId(UUID id) {
        return id.toString().substring(0, 8);
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

    private record ActivatedToken(UUID id, String apiKey) {
    }
}
