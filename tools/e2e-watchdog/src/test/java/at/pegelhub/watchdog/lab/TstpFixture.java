package at.pegelhub.watchdog.lab;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.xml.parsers.DocumentBuilderFactory;

/** Stateful raw TSTP storage: GET can only return measurements actually accepted by PUT. */
final class TstpFixture {
    private static final int RECORD_BYTES = 12;

    private record Point(Instant observed, float value, Instant received) {
    }

    private final TreeMap<Instant, Point> points = new TreeMap<>();
    String mode = "normal";
    int delaySeconds = 60;
    long puts;

    synchronized String handle(Map<String, String> query, byte[] body) throws Exception {
        return switch (query.getOrDefault("Cmd", "").toUpperCase()) {
            case "QUERY" -> {
                if (!"10001373".equals(query.get("ORT"))) {
                    throw new IllegalArgumentException("Unknown station");
                }
                yield "<TSQ RELEASE=\"1\"><TSATTR><ZRID>1</ZRID><MAXQUAL>0</MAXQUAL><WRITABLE>True</WRITABLE>"
                        + "<PARAMETER>Wasserstand</PARAMETER><ORT>10001373</ORT><DEFART>K</DEFART><HERKUNFT>O</HERKUNFT>"
                        + "<REIHENART>Z</REIHENART><EINHEIT>cm</EINHEIT><HAUPTREIHE>True</HAUPTREIHE></TSATTR></TSQ>";
            }
            case "PUT" -> put(body);
            case "GET" -> get(query);
            default -> throw new IllegalArgumentException("Unknown command");
        };
    }

    private String put(byte[] body) throws Exception {
        var input = decodePayload(body);
        while (input.hasRemaining()) {
            Instant time = readTimestamp(input);
            float value = input.getFloat();
            Point previous = points.get(time);

            // Connector overlap replays identical PUTs. Restarting their delay would hide them forever.
            Instant received = previous != null && previous.value == value ? previous.received : Instant.now();
            points.put(time, new Point(time, value, received));
        }
        puts++;
        return "<TSR RELEASE=\"1\">\nconfirm</TSR>\n";
    }

    private ByteBuffer decodePayload(byte[] body) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(body));
        byte[] bytes = Base64.getMimeDecoder().decode(document.getElementsByTagName("DATA").item(0).getTextContent());
        if (bytes.length % RECORD_BYTES != 0) {
            throw new IllegalArgumentException("Invalid TSTP data length");
        }
        return ByteBuffer.wrap(bytes);
    }

    /** Eight UTC calendar bytes precede each big-endian float; these are not epoch milliseconds. */
    private Instant readTimestamp(ByteBuffer input) {
        input.get();
        int year = (Byte.toUnsignedInt(input.get()) & 15) * 256 + Byte.toUnsignedInt(input.get());
        int month = Byte.toUnsignedInt(input.get());
        int day = Byte.toUnsignedInt(input.get());
        int hour = Byte.toUnsignedInt(input.get());
        int minute = Byte.toUnsignedInt(input.get());
        int second = Byte.toUnsignedInt(input.get());
        return LocalDateTime.of(year, month, day, hour, minute, second).toInstant(ZoneOffset.UTC);
    }

    private String get(Map<String, String> query) {
        Instant from = Instant.parse(query.get("Von"));
        Instant to = Instant.parse(query.get("Bis"));
        var visible = points.subMap(from, true, to, true).values().stream()
                .filter(point -> !mode.equals("drop"))
                .filter(point -> !mode.equals("delay") || !point.received.plusSeconds(delaySeconds).isAfter(Instant.now()))
                .toList();
        var output = ByteBuffer.allocate(visible.size() * RECORD_BYTES);
        for (Point point : visible) {
            writePoint(output, point);
        }
        return "<TSD RELEASE=\"1\"><DEF REIHENART=\"Z\" TEXT=\"Nein\" DEFART=\"K\" EINHEIT=\"cm\" LEN=\""
                + output.capacity() + "\" ANZ=\"" + visible.size() + "\"/><DATA><![CDATA["
                + Base64.getEncoder().encodeToString(output.array()) + "]]></DATA></TSD>";
    }

    private void writePoint(ByteBuffer output, Point point) {
        var utc = point.observed.atOffset(ZoneOffset.UTC);
        output.put((byte) 0).put((byte) (utc.getYear() >> 8)).put((byte) utc.getYear())
                .put((byte) utc.getMonthValue()).put((byte) utc.getDayOfMonth()).put((byte) utc.getHour())
                .put((byte) utc.getMinute()).put((byte) utc.getSecond());
        // Preserve timestamps to demonstrate that a freshness check deliberately cannot detect changed values.
        output.putFloat(point.value + (mode.equals("corrupt") ? 1 : 0));
    }

    synchronized Map<String, Object> status() {
        return Map.of("mode", mode, "points", points.size(), "puts", puts);
    }

    synchronized void configure(String mode, int delay) {
        if (!Set.of("normal", "drop", "corrupt", "delay").contains(mode) || delay < 0) {
            throw new IllegalArgumentException();
        }
        this.mode = mode;
        this.delaySeconds = delay;
    }
}
