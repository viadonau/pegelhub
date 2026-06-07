package at.pegelhub.connector.tstp.service.impl;

import at.pegelhub.lib.model.Measurement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TstpBinaryServiceImplTest {
    private TstpBinaryServiceImpl tstpBinaryService;

    @BeforeEach
    public void setUp() {
        tstpBinaryService = new TstpBinaryServiceImpl();
    }

    @Test
    public void testDecode_binaryWithOneMeasurement_listWithOneMeasurement() {
        byte[] toDecode = new byte[]{
                0, 7, -38, 8, 3, 13, 30, 0, 68, 38, 44, -51
        };
        List<Measurement> measurements = tstpBinaryService.decode(toDecode);

        assertEquals(1, measurements.size());
        Measurement measurement = measurements.get(0);
        assertEquals(Instant.parse("2010-08-03T13:30:00Z"), measurement.getObservedAt());
        assertEquals(664.7, measurement.getValue());
    }

    @Test
    public void testEncode_oneMeasurement_binaryWithOneMeasurement() {
        List<Measurement> measurements = new ArrayList<>();
        Instant dateTime = Instant.parse("2010-08-03T13:30:00Z");
        measurements.add(new Measurement(null, dateTime, 664.7));

        byte[] encodedBytes = tstpBinaryService.encode(measurements);

        byte[] expectedBytes = new byte[]{
                0, 7, -38, 8, 3, 13, 30, 0, 68, 38, 44, -51
        };

        assertArrayEquals(expectedBytes, encodedBytes);
    }

    @Test
    public void testDecode_emptyBinary_emptyList() {
        byte[] toDecode = new byte[0];
        List<Measurement> measurements = tstpBinaryService.decode(toDecode);

        assertEquals(0, measurements.size());
    }

    @Test
    public void testEncode_emptyList_emptyBinary() {
        List<Measurement> measurements = new ArrayList<>();
        byte[] encodedBytes = tstpBinaryService.encode(measurements);

        assertArrayEquals(new byte[0], encodedBytes);
    }

    @Test
    public void testDecode_invalidBinaryLength_emptyList() {
        byte[] toDecode = new byte[]{0, 1, 0x07};
        List<Measurement> measurements = tstpBinaryService.decode(toDecode);

        assertEquals(0, measurements.size());
    }

    @Test
    public void testEncode_listWithMultipleMeasurements_binaryWithMultipleMeasurements() {
        List<Measurement> measurements = new ArrayList<>();
        Instant dateTime1 = Instant.parse("2010-08-03T13:30:00Z");
        Instant dateTime2 = Instant.parse("2024-05-24T07:44:37Z");
        measurements.add(new Measurement(null, dateTime1, 664.7));
        measurements.add(new Measurement(null, dateTime2, 351.0));

        byte[] encodedBytes = tstpBinaryService.encode(measurements);

        byte[] expectedBytes = new byte[]{
                0, 7, -38, 8, 3, 13, 30, 0, 68, 38, 44, -51,
                0, 7, -24, 5, 24, 7, 44, 37, 67, -81, -128, 0
        };

        assertArrayEquals(expectedBytes, encodedBytes);
    }

    @Test
    public void testDecode_binaryWithMultipleMeasurements_listWithMultipleMeasurements() {
        byte[] toDecode = new byte[]{
                0, 7, -38, 8, 3, 13, 30, 0, 68, 38, 44, -51,
                0, 7, -24, 5, 24, 7, 44, 37, 67, -81, -128, 0
        };
        List<Measurement> measurements = tstpBinaryService.decode(toDecode);

        assertEquals(2, measurements.size());
        Measurement measurement1 = measurements.get(0);
        assertEquals(Instant.parse("2010-08-03T13:30:00Z"), measurement1.getObservedAt());
        assertEquals(664.7, measurement1.getValue());
        Measurement measurement2 = measurements.get(1);
        assertEquals(Instant.parse("2024-05-24T07:44:37Z"), measurement2.getObservedAt());
        assertEquals(351.0, measurement2.getValue());
    }
}
