package at.pegelhub.connector.tstp.config;

import at.pegelhub.lib.config.ConfigValidation;

import java.time.DateTimeException;
import java.time.ZoneOffset;

public record TstpServer(
        String host,
        int port,
        String timeOffset,
        TstpWriteFormat writeFormat
) {
    public TstpServer(String host, int port, String timeOffset) {
        this(host, port, timeOffset, TstpWriteFormat.BINARY);
    }

    public TstpServer {
        writeFormat = writeFormat == null ? TstpWriteFormat.BINARY : writeFormat;
        host = ConfigValidation.requireText(host, "tstp.server.host");
        port = ConfigValidation.requireTcpPort(port, "tstp.server.port");
        try {
            timeOffset = ZoneOffset.of(timeOffset == null ? "Z" : timeOffset).getId();
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("tstp.server.timeOffset must be a fixed UTC offset, e.g. Z or +01:00", e);
        }
    }
}
