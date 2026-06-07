package at.pegelhub.testsupport;

import at.pegelhub.connector.domain.Connector;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.contact.domain.Contact;
import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.measurement.domain.MeasurementAverage;
import at.pegelhub.telemetry.domain.Telemetry;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.assertj.core.util.VisibleForTesting;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@VisibleForTesting
public final class ExampleData {

    private ExampleData() {
        throw new IllegalStateException("utility class can not be initialized.");
    }

    public static final UUID ID = UUID.fromString("d7305ab2-0b3d-4081-914a-e2c6047c1e12");
    public static final String MEASUREMENT_ID = "a93fdc3d-b71f-44ce-a826-fe1dc1f1f357";
    public static final String TIMESTAMP = "2010-10-12T08:50:00Z";
    public static final String IP_ADDRESS = "172.0.0.0";
    public static final int CYCLE_TIME = 1;
    public static final double TEMPERATURE = -2.0;
    public static final double PERFORMANCE = 2.0;
    public static final double FIELD_STRENGTH = 2.0;

    public static final Measurement MEASUREMENT = new Measurement(
            new TimeSeriesId(UUID.fromString(MEASUREMENT_ID)),
            Instant.parse(TIMESTAMP),
            Instant.parse(TIMESTAMP).plusSeconds(1),
            1.0,
            new ConnectorId(ID));
    public static final List<Measurement> MEASUREMENTS = List.of(MEASUREMENT);
    public static final MeasurementAverage MEASUREMENT_AVERAGE = new MeasurementAverage(
            MEASUREMENT.timeSeriesId(),
            Instant.parse(TIMESTAMP),
            Instant.parse(TIMESTAMP).plusSeconds(3600),
            1.0,
            1);
    public static final Telemetry TELEMETRY = new Telemetry(MEASUREMENT_ID, IP_ADDRESS, IP_ADDRESS,
            Instant.parse(TIMESTAMP), CYCLE_TIME, TEMPERATURE, TEMPERATURE, PERFORMANCE, PERFORMANCE,
            PERFORMANCE, PERFORMANCE, FIELD_STRENGTH);
    public static final List<Telemetry> TELEMETRIES = List.of(TELEMETRY);

    public static final String ORGANIZATION = "org1";
    public static final String CONTACT_PERSON = "Hans Maier";
    public static final String CONTACT_STREET = "Blumenweg 22";
    public static final String CONTACT_PLZ = "1549";
    public static final String LOCATION = "Wien";
    public static final String CONTACT_COUNTRY = "AT";
    public static final String EMERGENCY_NUMBER = "123456789";
    public static final String EMERGENCY_NUMBER_TWO = "123456780";
    public static final String EMERGENCY_MAIL = "emergency@mail.com";
    public static final String SERVICE_NUMBER = "123456789";
    public static final String SERVICE_NUMBER_TWO = "123456780";
    public static final String SERVICE_MAIL = "service@mail.com";
    public static final String ADMIN_NUMBER = "123456789";
    public static final String ADMIN_NUMBER_TWO = "123456780";
    public static final String ADMIN_MAIL = "service@mail.com";
    public static final String NOTES = "notes";
    public static final String CONNECTOR_NUMBER = "connectorNR";
    public static final String DESCRIPTION = "description";
    public static final String VERSION = "1.0.0";
    public static final String DATA_DEFINITION = "definition";
    public static final String MANUFACTURER_NAME = "name";
    public static final String MANUFACTURER_TYPE = "type";
    public static final String REMARK = "remarks";
    public static final Integer STATION_ID = 4143365;
    public static final double ACCURACY = 1.0;
    public static final String MAIN_USAGE = "do smth.";
    public static final String DATA_CRITICALLY = "true";
    public static final double REFERENCE_LEVEL = 0.5;
    public static final String REFERENCE_PLACE = "place";
    public static final double WATER_KILOMETER = 50;
    public static final String WATER_SIDE = "side";
    public static final double WATER_LAT = 45.5;
    public static final double WATER_LONG = 45.5;
    public static final double HSW = 45.5;
    public static final Duration REFRESH_RATE = Duration.of(100, ChronoUnit.MILLIS);

    public static final Contact CONTACT = new Contact(ID, ORGANIZATION, CONTACT_PERSON, CONTACT_STREET,
            CONTACT_PLZ, LOCATION, CONTACT_COUNTRY, EMERGENCY_NUMBER, EMERGENCY_NUMBER_TWO, EMERGENCY_MAIL,
            SERVICE_NUMBER, SERVICE_NUMBER_TWO, SERVICE_MAIL, ADMIN_NUMBER, ADMIN_NUMBER_TWO, ADMIN_MAIL, NOTES);

    public static final Connector CONNECTOR = new Connector(
            new ConnectorId(ID), CONNECTOR_NUMBER, CONTACT, DESCRIPTION, VERSION, VERSION,
            DATA_DEFINITION, CONTACT, CONTACT, CONTACT, NOTES, null, null);
}
