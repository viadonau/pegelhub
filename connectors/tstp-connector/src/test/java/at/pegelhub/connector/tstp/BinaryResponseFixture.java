package at.pegelhub.connector.tstp;

import at.pegelhub.connector.tstp.codec.TstpBinaryCodec;
import at.pegelhub.lib.model.Measurement;
import java.util.Base64;
import java.util.List;

public final class BinaryResponseFixture {
    private BinaryResponseFixture() { }

    public static String response(List<Measurement> measurements, String unit) {
        byte[] bytes = new TstpBinaryCodec().encode(measurements);
        return "<TSD RELEASE=\"1\"><DEF REIHENART=\"Z\" TEXT=\"Nein\" DEFART=\"K\" EINHEIT=\""
                + unit + "\" LEN=\"" + bytes.length + "\" ANZ=\"" + measurements.size()
                + "\"/><DATA>" + Base64.getEncoder().encodeToString(bytes) + "</DATA></TSD>";
    }
}
