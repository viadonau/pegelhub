package at.pegelhub.connector.tstp.codec;

import at.pegelhub.lib.model.Measurement;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TstpCodecTest {
    private final TstpBinaryCodec binary = new TstpBinaryCodec();
    private final TstpXmlCodec xml = new TstpXmlCodec(binary);

    @Test
    void binaryRoundTripPreservesTimestampAndFloatValue() {
        Measurement input = new Measurement(null, Instant.parse("2026-06-07T10:15:30Z"), 42.125);

        List<Measurement> decoded = binary.decode(binary.encode(List.of(input)));

        assertEquals(1, decoded.size());
        assertEquals(input.getObservedAt(), decoded.getFirst().getObservedAt());
        assertEquals(42.13, decoded.getFirst().getValue());
    }

    @Test
    void rejectsTruncatedBinaryPayload() {
        assertThrows(IllegalArgumentException.class, () -> binary.decode(new byte[11]));
    }

    @Test
    void writesAndReadsMeasurementXml() {
        Measurement input = new Measurement(null, Instant.parse("2026-06-07T10:15:30Z"), 7.5);

        String request = xml.writeRequest(List.of(input));
        List<Measurement> decoded = xml.parseMeasurements(request);

        assertTrue(request.contains("ANZ=\"1\""));
        assertEquals(7.5, decoded.getFirst().getValue());
    }
}
