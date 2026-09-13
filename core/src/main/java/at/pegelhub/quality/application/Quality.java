package at.pegelhub.quality.application;

import at.pegelhub.measurement.application.InternalMeasurements;
import at.pegelhub.measurement.domain.InternalProducerId;
import at.pegelhub.notifications.application.Notifications;
import at.pegelhub.quality.domain.Finding;
import at.pegelhub.quality.domain.ProfileConfig;
import at.pegelhub.quality.domain.QualityEvaluator;
import at.pegelhub.quality.domain.QualityProfile;
import at.pegelhub.quality.domain.QualityRun;
import at.pegelhub.quality.persistence.QualityRepository;
import at.pegelhub.shared.api.Page;
import at.pegelhub.shared.error.MetadataConflictException;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Application interface for profile configuration and advisory results. A profile UUID also
 * identifies its internal producer, so output ownership survives edits and process restarts.
 * Configuration and result methods commit only PostgreSQL state; Influx output belongs to the worker.
 */
@Service
public class Quality {

    private static final int NOTIFICATION_SUMMARY_LIMIT = 7900;

    private final QualityRepository repository;
    private final InternalMeasurements measurements;
    private final Notifications notifications;
    private final QualityProperties properties;
    private final Clock clock;

    public Quality(
            QualityRepository repository,
            InternalMeasurements measurements,
            Notifications notifications,
            QualityProperties properties,
            Clock clock) {
        this.repository = repository;
        this.measurements = measurements;
        this.notifications = notifications;
        this.properties = properties;
        this.clock = clock;
    }

    public List<QualityProfile> profiles() {
        return repository.profiles();
    }

    public QualityProfile profile(UUID id) {
        return repository.profile(id, false);
    }

    public Page<QualityRun> runs(UUID profile, int offset, int limit) {
        return repository.runs(profile, offset, limit);
    }

    public QualityRun run(UUID id) {
        return repository.run(id);
    }

    public Page<Finding> findings(UUID run, int offset, int limit) {
        return repository.findings(run, offset, limit);
    }

    @Transactional
    public QualityProfile create(ProfileConfig config) {
        return save(UUID.randomUUID(), config, true);
    }

    @Transactional
    public QualityProfile update(UUID id, ProfileConfig config) {
        var existing = repository.profile(id, true);

        // Keep bindings stable from claim through release; the run's measurement access uses its producer ID.
        if (existing.currentRunId() != null) {
            throw new MetadataConflictException("QA run is in progress");
        }

        // An inactive source must not prevent an operator from disabling its unchanged profile.
        boolean reconfigure = config.enabled() || !config.sources().equals(existing.configuration().sources())
                || !Objects.equals(config.output(), existing.configuration().output());

        return save(id, config, reconfigure);
    }

    private QualityProfile save(UUID id, ProfileConfig config, boolean reconfigure) {
        notifications.requireDestinations(config.destinations());

        if (reconfigure) {
            measurements.configure(
                    new InternalProducerId(id),
                    config.sources().stream().map(TimeSeriesId::new).toList(),
                    config.output() == null ? null : new TimeSeriesId(config.output()));
        }

        repository.save(id, config, clock.instant().plusSeconds(config.intervalSeconds()));
        return repository.profile(id, false);
    }

    /** Requests the next execution slot; it neither executes synchronously nor creates a historical run. */
    @Transactional
    public void requestRun(UUID id) {
        var profile = repository.profile(id, true);
        if (!properties.enabled() || !profile.configuration().enabled()) {
            throw new MetadataConflictException("QA execution is disabled");
        }
        if (profile.currentRunId() != null || profile.runRequested()) {
            throw new MetadataConflictException("QA run is already pending or running");
        }

        repository.requestRun(id);
    }

    /**
     * Commits findings and per-destination delivery submissions together. Call once for a claimed
     * run, after complete-read validation; an uncertain commit must be recovered, not overwritten
     * by {@link #fail(QualityRun, QualityRun.State, String)}. A PENDING output records intent,
     * not a successful Influx write.
     */
    @Transactional
    public void record(QualityRun run, QualityEvaluator.Result result) {
        var state = !result.complete() ? QualityRun.State.INCOMPLETE
                : result.findings().isEmpty() ? QualityRun.State.PASSED : QualityRun.State.FINDINGS;
        var output = run.configuration().output() == null ? QualityRun.OutputState.NOT_CONFIGURED
                : result.output() == null ? QualityRun.OutputState.SUPPRESSED : QualityRun.OutputState.PENDING;

        repository.finish(run.id(), state, result.findings(), output, null, clock.instant());

        if (!result.findings().isEmpty() && !run.configuration().destinations().isEmpty()) {
            notifications.submitForRun(run.id(), run.configuration().destinations(),
                    "PH QA: " + run.configuration().name(), notificationSummary(run, result.findings()));
        }
    }

    private String notificationSummary(QualityRun run, List<Finding> findings) {
        var body = new StringBuilder("QA: ").append(run.configuration().name())
                .append("\nLauf: ").append(run.id())
                .append("\nBefunde: ").append(findings.size())
                .append('\n');

        for (var finding : findings) {
            String line = finding.rule() + ": " + finding.message() + " " + finding.timeSeriesIds() + "\n";

            // Leave room for the truncation notice within the notification module's body limit.
            if (body.length() + line.length() > NOTIFICATION_SUMMARY_LIMIT) {
                body.append("Weitere Befunde in PH.");
                break;
            }

            body.append(line);
        }

        return body.toString();
    }

    /** Records a known pre-commit execution failure; not safe after an uncertain result commit. */
    @Transactional
    public void fail(QualityRun run, QualityRun.State state, String error) {
        repository.finish(
                run.id(), state, List.of(),
                run.configuration().output() == null
                        ? QualityRun.OutputState.NOT_CONFIGURED : QualityRun.OutputState.SUPPRESSED,
                error, clock.instant());
    }
}
