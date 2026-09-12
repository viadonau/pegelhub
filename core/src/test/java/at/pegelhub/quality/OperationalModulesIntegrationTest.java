package at.pegelhub.quality;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measurement.application.InternalMeasurements;
import at.pegelhub.measurement.application.MeasurementListQuery;
import at.pegelhub.measurement.application.MeasurementOrder;
import at.pegelhub.measurement.application.MeasurementReadRow;
import at.pegelhub.measurement.application.MeasurementWindow;
import at.pegelhub.measurement.domain.InternalProducerId;
import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.measurement.persistence.MeasurementRepository;
import at.pegelhub.notifications.application.CredentialCatalog;
import at.pegelhub.notifications.application.NotificationProperties;
import at.pegelhub.notifications.application.Notifications;
import at.pegelhub.notifications.domain.Delivery;
import at.pegelhub.notifications.domain.DestinationConfig;
import at.pegelhub.notifications.persistence.NotificationRepository;
import at.pegelhub.notifications.persistence.SnmpEngineRepository;
import at.pegelhub.quality.application.Quality;
import at.pegelhub.quality.domain.*;
import at.pegelhub.quality.persistence.QualityRepository;
import at.pegelhub.shared.error.MetadataConflictException;
import at.pegelhub.shared.metadata.MetadataStatus;
import at.pegelhub.testsupport.FullStackIntegrationTestBase;
import at.pegelhub.timeseries.application.TimeSeriesService;
import at.pegelhub.timeseries.application.UpdateTimeSeriesCommand;
import at.pegelhub.timeseries.domain.SourceAssignment;
import at.pegelhub.timeseries.domain.MeasurementRepresentation;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

@TestPropertySource(properties = {"pegelhub.notifications.credentials.test.kind=SMTP",
        "pegelhub.notifications.credentials.test.host=localhost", "pegelhub.notifications.credentials.test.port=9",
        "pegelhub.notifications.credentials.test.start-tls=false"})
class OperationalModulesIntegrationTest extends FullStackIntegrationTestBase {
    @MockitoBean
    JwtDecoder jwtDecoder;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    Quality quality;

    @Autowired
    QualityRepository runs;

    @Autowired
    Notifications notifications;

    @Autowired
    NotificationRepository deliveries;

    @Autowired
    SnmpEngineRepository engines;

    @Autowired
    InternalMeasurements internal;

    @Autowired
    MeasurementRepository measurements;

    @Autowired
    TimeSeriesService catalog;

    @Autowired
    PlatformTransactionManager transactions;

    @Autowired
    Clock clock;

    @org.springframework.boot.test.web.server.LocalManagementPort
    int managementPort;

    private UUID station;
    private ConnectorId connector;

    @BeforeEach
    void catalog() {
        var owner = UUID.randomUUID();
        station = UUID.randomUUID();
        connector = new ConnectorId(UUID.randomUUID());

        jdbc.update("insert into station_owner(id,name) values (?, 'Owner')", owner);
        jdbc.update("insert into station(id,owner_id,name,water_body) values (?,?,'Station','Danube')", station, owner);
        jdbc.update("insert into connector(id,name,type,status) values (?,'Connector','other','active')", connector.value());
    }

    @Test
    void internalOutputIsExclusiveStableAndReadableAlongsideLegacyMeasurements() {
        var input = series(true);
        var output = series(false);
        var profile = quality.create(config("Derived", input, output, List.of()));
        var producer = new InternalProducerId(profile.id());
        var time = Instant.now().minusSeconds(60).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        measurements.storeMeasurements(List.of(new Measurement(input, time, time, 8, connector)));

        var window = new MeasurementWindow(time.minusSeconds(1), time.plusSeconds(1), null);
        var data = internal.readWindow(producer, window, 100, Duration.ofSeconds(5));
        var result = QualityEvaluator.evaluate(profile.configuration(), data);

        assertThat(result.output().value()).isEqualTo(8);

        internal.writeOutput(producer, output, time, 8, Duration.ofSeconds(5));
        internal.writeOutput(producer, output, time, 9, Duration.ofSeconds(5));

        assertThat(measurements.listMeasurements(new MeasurementListQuery(output, window, MeasurementOrder.ASC, 100)).measurements())
                .singleElement().satisfies(row -> {
                    assertThat(row.value()).isEqualTo(9);
                    assertThat(row.submittedByInternalProducerId()).isEqualTo(producer);
                });
        assertThatThrownBy(() -> internal.writeOutput(producer, input, time, 1, Duration.ofSeconds(5)))
                .isInstanceOf(MetadataConflictException.class);
        assertThatThrownBy(() -> quality.create(config("Competing", input, output, List.of())))
                .isInstanceOf(MetadataConflictException.class);

        var next = series(false);
        var other = quality.create(config("Dependent", output, next, List.of()));

        assertThatThrownBy(() -> quality.update(profile.id(), config("Derived", next, output, List.of())))
                .isInstanceOf(MetadataConflictException.class);
        assertThat(catalog.get(next).sourceAssignment().internalProducerId().value()).isEqualTo(other.id());
    }

    @Test
    void qaUsesCanonicalValuesAcrossDifferentConnectorRepresentations() {
        var litres = series(true);
        var canonical = series(true);
        var output = series(false);
        for (var id : List.of(litres, canonical, output)) {
            jdbc.update("update time_series set observed_property = 'discharge' where id = ?", id.value());
        }
        catalog.update(litres, new UpdateTimeSeriesCommand(
                MetadataStatus.ACTIVE, new SourceAssignment(connector, MeasurementRepresentation.LITRES_PER_SECOND)));
        var profile = quality.create(new ProfileConfig(
                "Discharge", true, List.of(litres.value(), canonical.value()),
                new QualityRules(new QualityRules.Range(0, 2), null, null, null),
                1200, 60, List.of(), output.value()));
        var producer = new InternalProducerId(profile.id());
        var time = clock.instant().minusSeconds(60).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        measurements.storeMeasurements(List.of(
                new Measurement(litres, time, time, 1.25, connector),
                new Measurement(canonical, time, time, 1.75, connector)));

        var window = new MeasurementWindow(time.minusSeconds(1), time.plusSeconds(1), null);
        var data = internal.readWindow(producer, window, 100, Duration.ofSeconds(5));
        var result = QualityEvaluator.evaluate(profile.configuration(), data);

        assertThat(result.complete()).isTrue();
        assertThat(result.findings()).isEmpty();
        assertThat(result.output()).isEqualTo(new QualityEvaluator.Output(time, 1.5));
        assertThat(catalog.get(litres).sourceRepresentation()).isEqualTo(MeasurementRepresentation.LITRES_PER_SECOND);
        assertThat(catalog.get(output).sourceAssignment()).isEqualTo(SourceAssignment.internal(producer));

        internal.writeOutput(producer, output, result.output().observedAt(), result.output().value(), Duration.ofSeconds(5));

        assertThat(measurements.listMeasurements(new MeasurementListQuery(output, window, MeasurementOrder.ASC, 100))
                .measurements()).containsExactly(new MeasurementReadRow(time, 1.5, null, producer));
    }

    @Test
    void findingsAndDeliveriesCommitTogetherAndEachRunNotifiesAgain() {
        var input = series(true);
        var target = notifications.create(destination(true));
        var profile = quality.create(config("Advisory", input, null, List.of(target.id())));
        var run = runs.claim(clock.instant().plusSeconds(120));
        var finding = new Finding(Finding.Rule.RANGE, Finding.Severity.FAILED, List.of(input.value()),
                clock.instant(), "Out of range", Map.of("value", 100.0));
        var result = new QualityEvaluator.Result(List.of(finding), null, true);

        assertThatThrownBy(() -> new TransactionTemplate(transactions).execute(status -> {
            quality.record(run, result);
            throw new IllegalStateException("rollback");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(runs.run(run.id()).state()).isEqualTo(QualityRun.State.RUNNING);
        assertThat(deliveries.history(0, 25).total()).isZero();

        quality.record(run, result);
        runs.release(run, clock.instant());
        var second = runs.claim(clock.instant().plusSeconds(120));
        quality.record(second, result);

        assertThat(deliveries.history(0, 25).total()).isEqualTo(2);
        assertThat(runs.findings(run.id(), 0, 25).total()).isEqualTo(1);
        assertThat(runs.run(run.id()).configuration()).isEqualTo(profile.configuration());
    }

    @Test
    void pointLimitRejectsIncompleteReadsInsteadOfPassing() {
        var input = series(true);
        var profile = quality.create(config("Bounded", input, null, List.of()));
        var time = clock.instant().minusSeconds(60);
        measurements.storeMeasurements(List.of(new Measurement(input, time, time, 1, connector),
                new Measurement(input, time.plusSeconds(1), time, 2, connector)));

        assertThatThrownBy(() -> internal.readWindow(new InternalProducerId(profile.id()),
                new MeasurementWindow(time.minusSeconds(1), time.plusSeconds(2), null), 1, Duration.ofSeconds(5)))
                .isInstanceOf(at.pegelhub.measurement.application.MeasurementReadLimitException.class);
    }

    @Test
    void inFlightAcceptanceSurvivesDisablingAndPendingWorkIsCancelled() {
        var target = notifications.create(destination(true));
        notifications.submit(List.of(target.id()), "First", "Body");
        notifications.submit(List.of(target.id()), "Second", "Body");

        var inFlight = deliveries.claim(clock.instant().plusSeconds(1));
        jdbc.update("update notification_delivery set lease_until = ? where id = ?",
                java.sql.Timestamp.from(clock.instant().minusSeconds(1)), inFlight.id());

        notifications.update(target.id(), destination(false));
        deliveries.finish(inFlight, Delivery.State.ACCEPTED, null, clock.instant(), clock.instant());

        assertThat(deliveries.get(inFlight.id()).state()).isEqualTo(Delivery.State.ACCEPTED);
        assertThat(deliveries.history(0, 25).items()).extracting(Delivery::state)
                .containsExactlyInAnyOrder(Delivery.State.ACCEPTED, Delivery.State.CANCELLED);
    }

    @Test
    void routingIsSnapshottedRetryLeaseRecoversAndFiveAttemptsAreBounded() {
        var target = notifications.create(destination(true));
        notifications.submit(List.of(target.id()), "Subject", "Body");
        var original = deliveries.history(0, 25).items().getFirst();

        var changed = new DestinationConfig("Changed", true, DestinationConfig.Transport.SMTP, "test",
                new DestinationConfig.MailRoute("other@example.test", List.of("new@example.test"), null), null);
        notifications.update(target.id(), changed);

        assertThat(deliveries.get(original.id()).route().mail().recipients()).containsExactly("receiver@example.test");

        var now = clock.instant().plusSeconds(1);
        for (int i = 1; i <= 5; i++) {
            var claim = deliveries.claim(now);

            assertThat(claim.id()).isEqualTo(original.id());
            assertThat(claim.attempts()).isEqualTo(i);

            now = now.plusSeconds(301);
        }

        assertThat(deliveries.claim(now)).isNull();
        assertThat(deliveries.get(original.id()).state()).isEqualTo(Delivery.State.FAILED);
    }

    @Test
    void optionalCredentialRemovalDoesNotPreventDisabling() {
        var target = notifications.create(destination(true));
        var unavailable = new Notifications(
                deliveries, new CredentialCatalog(new NotificationProperties(false, 90, Map.of())), clock);

        new TransactionTemplate(transactions).executeWithoutResult(status -> unavailable.update(target.id(), destination(false)));

        assertThat(notifications.destination(target.id()).configuration().enabled()).isFalse();
    }

    @Test
    void profilesCanBeDisabledAfterTheirSourcesBecomeInactiveAndRecoveryDoesNotBackfill() {
        var input = series(true);
        var profile = quality.create(config("Profile", input, null, List.of()));

        jdbc.update("update station set status = 'inactive' where id = ?", station);
        var c = profile.configuration();
        quality.update(profile.id(), new ProfileConfig(c.name(), false, c.sources(), c.rules(),
                c.lookbackSeconds(), c.intervalSeconds(), c.destinations(), c.output()));

        assertThat(quality.profile(profile.id()).configuration().enabled()).isFalse();

        jdbc.update("update station set status = 'active' where id = ?", station);
        quality.update(profile.id(), c);
        var run = runs.claim(clock.instant().plusSeconds(120));
        runs.recover(clock.instant());

        assertThat(runs.run(run.id()).state()).isEqualTo(QualityRun.State.INTERRUPTED);
        assertThat(runs.claim(clock.instant())).isNull();
    }

    @Test
    void snmpEngineIdentitySurvivesBoots() {
        var first = engines.boot();
        var second = engines.boot();

        assertThat(second.id()).containsExactly(first.id());
        assertThat(second.boots()).isEqualTo(first.boots() + 1);
    }

    @Test
    void slowSmtpDoesNotBlockMeasurementStorageOrHealth() throws Exception {
        var input = series(true);
        var target = notifications.create(destination(true));
        notifications.submit(List.of(target.id()), "Slow transport", "Body");

        try (var server = new java.net.ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress());
             var executor = Executors.newSingleThreadExecutor()) {
            server.setSoTimeout(5000);
            var credential = new NotificationProperties.Credential("SMTP", "localhost", server.getLocalPort(), false, false,
                    null, null, null, null, null, null, null, null, false);
            var properties = new NotificationProperties(true, 90, Map.of("test", credential));
            var sender = new at.pegelhub.notifications.application.NotificationWorker(properties, deliveries,
                    new at.pegelhub.notifications.transport.SmtpDeliveryTransport(new CredentialCatalog(properties)),
                    org.mockito.Mockito.mock(at.pegelhub.notifications.transport.SnmpDeliveryTransport.class), clock);

            var task = executor.submit(sender::tick);
            try (var stalledConnection = server.accept()) {
                var start = System.nanoTime();
                var now = clock.instant();
                measurements.storeMeasurements(List.of(new Measurement(input, now, now, 1, connector)));
                var health = rest.getForEntity("http://localhost:" + managementPort + "/actuator/health", String.class);

                assertThat(health.getStatusCode().value()).isEqualTo(200);
                assertThat(health.getBody()).contains("UP");
                assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(3));
                assertThat(task.isDone()).isFalse();
            }

            task.get(5, TimeUnit.SECONDS);

            var pending = deliveries.history(0, 25).items().getFirst();
            assertThat(pending.state()).isEqualTo(Delivery.State.PENDING);
            assertThat(pending.attempts()).isEqualTo(1);
            assertThat(pending.nextAttemptAt()).isAfter(pending.createdAt().plusSeconds(29));
        }
    }

    @Test
    void retentionKeepsPendingReferencesAndPurgesDeliveriesBeforeRuns() {
        var input = series(true);
        var target = notifications.create(destination(true));
        quality.create(config("Retention", input, null, List.of(target.id())));
        var run = runs.claim(clock.instant().plusSeconds(120));
        quality.record(run, new QualityEvaluator.Result(List.of(new Finding(Finding.Rule.DATA, Finding.Severity.WARNING,
                List.of(input.value()), null, "Missing data", Map.of())), null, false));
        runs.release(run, clock.instant());

        var before = clock.instant().plusSeconds(1);
        deliveries.prune(before);

        assertThat(notifications.retainsSource(run.id())).isTrue();
        assertThat(runs.expired(before, null)).contains(run.id());

        var delivery = deliveries.claim(before);
        deliveries.finish(delivery, Delivery.State.ACCEPTED, null, clock.instant(), clock.instant());

        assertThat(notifications.retainsSource(run.id())).isTrue();

        deliveries.prune(before);
        assertThat(notifications.retainsSource(run.id())).isFalse();

        runs.deleteRun(run.id());

        assertThat(runs.runs(null, 0, 25).total()).isZero();
        assertThat(jdbc.queryForObject("select count(*) from quality_finding", Long.class)).isZero();
    }

    @Test
    void cachedMetadataCannotOverwriteConcurrentInternalAssignment() throws Exception {
        var input = series(true);
        var output = series(false);
        var loaded = new CountDownLatch(1);
        var proceed = new CountDownLatch(1);

        try (var executor = Executors.newSingleThreadExecutor()) {
            var future = executor.submit(() -> {
                assertThatThrownBy(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
                    catalog.get(output);
                    loaded.countDown();
                    await(proceed);

                    catalog.update(output, new UpdateTimeSeriesCommand(
                            MetadataStatus.ACTIVE, new SourceAssignment(connector, MeasurementRepresentation.CANONICAL)));
                })).isInstanceOf(MetadataConflictException.class);
            });

            try {
                assertThat(loaded.await(10, TimeUnit.SECONDS)).isTrue();

                var profile = quality.create(config("Concurrent", input, output, List.of()));
                proceed.countDown();
                future.get(10, TimeUnit.SECONDS);

                assertThat(catalog.get(output).sourceAssignment().internalProducerId().value()).isEqualTo(profile.id());
            } finally {
                proceed.countDown();
            }
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Latch timed out");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private TimeSeriesId series(boolean raw) {
        var point = UUID.randomUUID();
        var id = UUID.randomUUID();

        jdbc.update("insert into measuring_point(id,station_id,name) values (?, ?, ?)", point, station, point.toString());
        jdbc.update("insert into time_series(id,measuring_point_id,observed_property,source_connector_id,source_representation) values (?,?,'water-level',?,?)",
                id, point, raw ? connector.value() : null, raw ? "canonical" : null);

        return new TimeSeriesId(id);
    }

    private ProfileConfig config(String name, TimeSeriesId source, TimeSeriesId output, List<UUID> destinations) {
        return new ProfileConfig(
                name, true, List.of(source.value()), null, 1200, 60, destinations,
                output == null ? null : output.value());
    }

    private DestinationConfig destination(boolean enabled) {
        return new DestinationConfig("SMTP", enabled, DestinationConfig.Transport.SMTP, "test",
                new DestinationConfig.MailRoute("sender@example.test", List.of("receiver@example.test"), null), null);
    }
}
