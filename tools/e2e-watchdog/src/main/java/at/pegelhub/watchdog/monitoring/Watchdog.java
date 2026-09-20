package at.pegelhub.watchdog.monitoring;

import at.pegelhub.lib.PegelHubClient;
import at.pegelhub.lib.model.Measurement;
import at.pegelhub.watchdog.config.WatchdogConfig;
import at.pegelhub.watchdog.state.StateStore;

import java.time.Clock;
import java.time.Instant;

/** Read-only monitoring. Re-reading an old value must not renew its freshness deadline. */
public final class Watchdog {
    private final WatchdogConfig config;
    private final StateStore store;
    private final PegelHubClient client;
    private final Clock clock;

    public Watchdog(WatchdogConfig config, StateStore store, PegelHubClient client, Clock clock) {
        this.config = config;
        this.store = store;
        this.client = client;
        this.clock = clock;
    }

    public void checkFreshness() {
        Measurement latest;
        try {
            latest = client.getLatestMeasurementOfTimeSeries(config.timeSeriesId()).orElse(null);
        } catch (Exception failure) {
            // Authentication, HTTP and decoding errors contain no usable evidence. Do not persist their bodies.
            store.record(CheckState.unknown("read_failed", clock.instant()));
            return;
        }

        // Evaluate after the read: time spent waiting for Core counts towards the measurement's age.
        Instant observedAt = latest == null ? null : latest.getObservedAt();
        CheckState result = Checks.evaluate(observedAt, clock.instant(), config.silence());
        store.record(result);
    }
}
