package at.pegelhub.shared.persistence;

import at.pegelhub.auth.persistence.JpaApiToken;
import at.pegelhub.auth.persistence.JpaApiTokenRepository;
import at.pegelhub.connector.persistence.JpaConnector;
import at.pegelhub.connector.persistence.JpaConnectorRepository;
import at.pegelhub.contact.persistence.JpaContact;
import at.pegelhub.contact.persistence.JpaContactRepository;
import at.pegelhub.supplier.persistence.JpaStationManufacturer;
import at.pegelhub.supplier.persistence.JpaStationManufacturerRepository;
import at.pegelhub.supplier.persistence.JpaSupplier;
import at.pegelhub.supplier.persistence.JpaSupplierRepository;
import at.pegelhub.taker.persistence.JpaTaker;
import at.pegelhub.taker.persistence.JpaTakerRepository;
import at.pegelhub.taker.persistence.JpaTakerServiceManufacturer;
import at.pegelhub.taker.persistence.JpaTakerServiceManufacturerRepository;
import at.pegelhub.testsupport.JpaIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
final class MetadataPersistenceIntegrationTest extends JpaIntegrationTestBase {

    @Autowired
    private JpaApiTokenRepository apiTokens;

    @Autowired
    private JpaContactRepository contacts;

    @Autowired
    private JpaConnectorRepository connectors;

    @Autowired
    private JpaStationManufacturerRepository stationManufacturers;

    @Autowired
    private JpaSupplierRepository suppliers;

    @Autowired
    private JpaTakerServiceManufacturerRepository takerServiceManufacturers;

    @Autowired
    private JpaTakerRepository takers;

    @Test
    void persistsSupplierAndTakerMetadataGraph() {
        UUID supplierTokenId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID takerTokenId = UUID.fromString("10000000-0000-0000-0000-000000000002");
        apiTokens.save(token(supplierTokenId));
        apiTokens.save(token(takerTokenId));

        JpaContact contact = contacts.save(contact(UUID.fromString("20000000-0000-0000-0000-000000000001")));
        JpaConnector supplierConnector = connectors.save(connector(
                UUID.fromString("30000000-0000-0000-0000-000000000001"),
                "supplier-connector",
                contact,
                supplierTokenId));
        JpaStationManufacturer stationManufacturer = stationManufacturers.save(new JpaStationManufacturer(
                UUID.fromString("40000000-0000-0000-0000-000000000001"),
                "station manufacturer",
                "station type",
                "1.0.0",
                "station remark"));
        JpaSupplier supplier = suppliers.saveAndFlush(supplier(
                UUID.fromString("50000000-0000-0000-0000-000000000001"),
                "supplier-station",
                supplierConnector,
                stationManufacturer));

        JpaConnector takerConnector = connectors.save(connector(
                UUID.fromString("30000000-0000-0000-0000-000000000002"),
                "taker-connector",
                contact,
                takerTokenId));
        JpaTakerServiceManufacturer takerServiceManufacturer = takerServiceManufacturers.save(new JpaTakerServiceManufacturer(
                UUID.fromString("60000000-0000-0000-0000-000000000001"),
                "taker manufacturer",
                "taker system",
                "1.0.0",
                "request remark"));
        JpaTaker taker = takers.saveAndFlush(taker(
                UUID.fromString("70000000-0000-0000-0000-000000000001"),
                takerConnector,
                takerServiceManufacturer));

        JpaSupplier persistedSupplier = suppliers.findById(supplier.getId()).orElseThrow();
        JpaTaker persistedTaker = takers.findById(taker.getId()).orElseThrow();

        assertEquals(supplier.getId(), suppliers.getSupplier(supplierTokenId));
        assertEquals(supplierConnector.getId(), persistedSupplier.getConnector().getId());
        assertEquals(stationManufacturer.getId(), persistedSupplier.getStationManufacturer().getId());
        assertEquals(takerConnector.getId(), persistedTaker.getConnector().getId());
        assertEquals(takerServiceManufacturer.getId(), persistedTaker.getTakerServiceManufacturer().getId());
        assertNotNull(connectors.findFirstByConnectorNumber("supplier-connector").orElseThrow().getManufacturer());
    }

    @Test
    void connectorApiTokenIsUnique() {
        UUID tokenId = UUID.fromString("10000000-0000-0000-0000-000000000003");
        apiTokens.save(token(tokenId));
        JpaContact contact = contacts.save(contact(UUID.fromString("20000000-0000-0000-0000-000000000003")));
        connectors.saveAndFlush(connector(
                UUID.fromString("30000000-0000-0000-0000-000000000003"),
                "connector-a",
                contact,
                tokenId));

        assertThrows(DataIntegrityViolationException.class, () -> connectors.saveAndFlush(connector(
                UUID.fromString("30000000-0000-0000-0000-000000000004"),
                "connector-b",
                contact,
                tokenId)));
    }

    @Test
    void referencedConnectorCannotBeDeletedWhileSupplierExists() {
        UUID tokenId = UUID.fromString("10000000-0000-0000-0000-000000000004");
        apiTokens.save(token(tokenId));
        JpaContact contact = contacts.save(contact(UUID.fromString("20000000-0000-0000-0000-000000000004")));
        JpaConnector connector = connectors.save(connector(
                UUID.fromString("30000000-0000-0000-0000-000000000005"),
                "connector-c",
                contact,
                tokenId));
        JpaStationManufacturer stationManufacturer = stationManufacturers.save(new JpaStationManufacturer(
                UUID.fromString("40000000-0000-0000-0000-000000000002"),
                "station manufacturer",
                "station type",
                "1.0.0",
                "station remark"));
        suppliers.saveAndFlush(supplier(
                UUID.fromString("50000000-0000-0000-0000-000000000002"),
                "supplier-station-c",
                connector,
                stationManufacturer));

        assertThrows(DataIntegrityViolationException.class, () -> {
            connectors.deleteById(connector.getId());
            connectors.flush();
        });
    }

    @Test
    void referencedStationManufacturerCannotBeDeletedWhileSupplierExists() {
        UUID tokenId = UUID.fromString("10000000-0000-0000-0000-000000000005");
        apiTokens.save(token(tokenId));
        JpaContact contact = contacts.save(contact(UUID.fromString("20000000-0000-0000-0000-000000000005")));
        JpaConnector connector = connectors.save(connector(
                UUID.fromString("30000000-0000-0000-0000-000000000006"),
                "connector-d",
                contact,
                tokenId));
        JpaStationManufacturer stationManufacturer = stationManufacturers.save(new JpaStationManufacturer(
                UUID.fromString("40000000-0000-0000-0000-000000000003"),
                "station manufacturer",
                "station type",
                "1.0.0",
                "station remark"));
        suppliers.saveAndFlush(supplier(
                UUID.fromString("50000000-0000-0000-0000-000000000003"),
                "supplier-station-d",
                connector,
                stationManufacturer));

        assertThrows(DataIntegrityViolationException.class, () -> {
            stationManufacturers.deleteById(stationManufacturer.getId());
            stationManufacturers.flush();
        });
    }

    private static JpaApiToken token(UUID id) {
        return new JpaApiToken(id, "hash-" + id, "salt-" + id, true, LocalDateTime.now().plusDays(1));
    }

    private static JpaContact contact(UUID id) {
        return new JpaContact(
                id,
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

    private static JpaConnector connector(UUID id, String connectorNumber, JpaContact contact, UUID apiToken) {
        return new JpaConnector(
                id,
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

    private static JpaSupplier supplier(
            UUID id,
            String stationNumber,
            JpaConnector connector,
            JpaStationManufacturer stationManufacturer) {
        return new JpaSupplier(
                id,
                stationNumber,
                101,
                "supplier station",
                "Danube",
                'W',
                stationManufacturer,
                connector,
                Duration.ofMinutes(5),
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

    private static JpaTaker taker(
            UUID id,
            JpaConnector connector,
            JpaTakerServiceManufacturer takerServiceManufacturer) {
        return new JpaTaker(
                id,
                "taker-station",
                201,
                takerServiceManufacturer,
                connector,
                Duration.ofMinutes(10));
    }
}
