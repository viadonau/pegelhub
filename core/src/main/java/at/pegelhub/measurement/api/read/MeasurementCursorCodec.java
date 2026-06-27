package at.pegelhub.measurement.api.read;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measurement.application.MeasurementCursor;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Encodes the opaque HTTP cursor for a stable Measurement page position.
 */
final class MeasurementCursorCodec {

    String encode(MeasurementCursor cursor) {
        if (cursor == null) {
            return null;
        }
        String rawCursor = cursor.observedAt() + "|" + cursor.submittedByConnectorId().value();
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(rawCursor.getBytes(StandardCharsets.UTF_8));
    }

    MeasurementCursor decode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(value);
            String[] parts = new String(decoded, StandardCharsets.UTF_8).split("\\|", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Cursor must contain observedAt and submittedByConnectorId");
            }
            return new MeasurementCursor(
                    Instant.parse(parts[0]),
                    new ConnectorId(UUID.fromString(parts[1])));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid cursor", ex);
        }
    }
}
