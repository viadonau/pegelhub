package at.pegelhub.connector.tstp.codec;

import at.pegelhub.lib.model.Measurement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.time.ZoneOffset;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
        assertEquals(42.125, decoded.getFirst().getValue());
    }

    @Test
    void rejectsTruncatedBinaryPayload() {
        assertThrows(IllegalArgumentException.class, () -> binary.decode(new byte[11]));
    }

    @Test
    void skipsOnlyTheProtocolGapMarkerWithoutDroppingItsNeighbors() {
        byte[] payload = HexFormat.of().parseHex(
                "0007ea09100c000000000000"
                        + "0007ea09100c0f007df0bdc2"
                        + "0007ea09100c1e00432a0000"
                        + "0007ea09100c2d007df0bdc1");

        List<Measurement> decoded = binary.decode(payload);

        assertEquals(List.of(
                        Instant.parse("2026-09-16T12:00:00Z"),
                        Instant.parse("2026-09-16T12:30:00Z"),
                        Instant.parse("2026-09-16T12:45:00Z")),
                decoded.stream().map(Measurement::getObservedAt).toList());
        assertEquals(List.of(0.0, 170.0, (double) Float.intBitsToFloat(0x7df0bdc1)),
                decoded.stream().map(Measurement::getValue).toList());
        assertTrue(binary.decode(HexFormat.of().parseHex("0007ea09100c0f007df0bdc2")).isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
            "+01:00,0007ea01010c000042280000,2026-01-01T11:00:00Z",
            "+01:00,0007ea07100c000042280000,2026-07-16T11:00:00Z",
            "+01:00,0007ea0101000f0042280000,2025-12-31T23:15:00Z",
            "-03:30,0007e90c1f17000042280000,2026-01-01T02:30:00Z"
    })
    void usesTheFixedOffsetInBothDirectionsIncludingDateRollover(String offset, String hex, Instant expected) {
        TstpBinaryCodec codec = new TstpBinaryCodec(ZoneOffset.of(offset));
        byte[] wire = HexFormat.of().parseHex(hex);

        assertEquals(expected, codec.decode(wire).getFirst().getObservedAt());
        assertArrayEquals(wire, codec.encode(List.of(new Measurement(null, expected, 42))));
    }

    @Test
    void doesNotSilentlyTreatNonFiniteValuesAsProtocolGaps() {
        for (String value : List.of("7fc00000", "7f800000", "ff800000")) {
            byte[] wire = HexFormat.of().parseHex("0007ea09100c0000" + value);
            assertThrows(IllegalArgumentException.class, () -> binary.decode(wire));
        }
    }

    @Test
    void writesAndReadsMeasurementXml() {
        Measurement input = new Measurement(null, Instant.parse("2026-06-07T10:15:30Z"), 7.5);

        String request = xml.writeRequest(List.of(input), "cm");
        List<Measurement> decoded = xml.parseMeasurements(request.getBytes(StandardCharsets.ISO_8859_1), "cm");

        assertTrue(request.contains("ANZ=\"1\""));
        assertEquals(7.5, decoded.getFirst().getValue());
    }
}
