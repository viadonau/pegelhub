package at.pegelhub.security;

import java.util.Arrays;
import java.util.Optional;

public enum PegelHubAuthority {
    MEASUREMENT_WRITE("measurement:write"),
    MEASUREMENT_READ("measurement:read"),
    TELEMETRY_WRITE("telemetry:write"),
    TELEMETRY_READ("telemetry:read"),
    METADATA_WRITE("metadata:write"),
    METADATA_READ("metadata:read"),
    SYSTEM_ADMIN("system:admin");

    private final String value;

    PegelHubAuthority(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static Optional<PegelHubAuthority> from(String value) {
        return Arrays.stream(values())
                .filter(authority -> authority.value.equals(value))
                .findFirst();
    }
}
