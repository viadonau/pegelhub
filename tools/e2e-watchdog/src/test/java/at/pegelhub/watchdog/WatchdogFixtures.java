package at.pegelhub.watchdog;

import at.pegelhub.watchdog.config.WatchdogConfig;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class WatchdogFixtures {
    public static final UUID BACK = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final Instant START = Instant.parse("2026-09-16T12:00:00Z");

    private WatchdogFixtures() {
    }

    public static WatchdogConfig config(int port, String version) {
        return config(List.of(new WatchdogConfig.Receiver("127.0.0.1", port)), version);
    }

    public static WatchdogConfig config(List<WatchdogConfig.Receiver> receivers, String version) {
        var core = new WatchdogConfig.Core("http://localhost/", "http://localhost/token", "watchdog", "secret");
        var snmp = new WatchdogConfig.Snmp(receivers, version,
                "1.3.6.1.4.1.32473.1", "1.3.6.1.4.1.32473.2", "community", "test", "auth", "privacy");
        return new WatchdogConfig("test", core, BACK, 600, 1L, snmp);
    }
}
