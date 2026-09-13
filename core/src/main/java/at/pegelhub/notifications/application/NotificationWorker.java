package at.pegelhub.notifications.application;

import at.pegelhub.notifications.domain.Delivery;
import at.pegelhub.notifications.domain.DestinationConfig;
import at.pegelhub.notifications.persistence.NotificationRepository;
import at.pegelhub.notifications.transport.SmtpDeliveryTransport;
import at.pegelhub.notifications.transport.SnmpDeliveryTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.temporal.ChronoUnit;

/**
 * Single-process sender, isolated from QA and ingestion. Claim/finish are short PostgreSQL
 * transactions; this worker must not be transactional around network calls. A lost transport
 * response is ambiguous, so retrying the same delivery can still cause recipient duplicates.
 */
@Component
public class NotificationWorker {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationWorker.class);

    private final NotificationProperties properties;
    private final NotificationRepository repository;
    private final SmtpDeliveryTransport smtp;
    private final SnmpDeliveryTransport snmp;
    private final Clock clock;

    public NotificationWorker(
            NotificationProperties properties,
            NotificationRepository repository,
            SmtpDeliveryTransport smtp,
            SnmpDeliveryTransport snmp,
            Clock clock) {
        this.properties = properties;
        this.repository = repository;
        this.smtp = smtp;
        this.snmp = snmp;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 1000, scheduler = "notificationScheduler")
    public void tick() {
        if (!properties.enabled()) {
            return;
        }

        var delivery = repository.claim(clock.instant());
        if (delivery == null) {
            return;
        }

        var now = clock.instant();
        if (!repository.destination(delivery.destinationId(), false).configuration().enabled()) {
            repository.finish(delivery, Delivery.State.CANCELLED, null, now, now);
            return;
        }

        try {
            send(delivery);
            repository.finish(delivery, Delivery.State.ACCEPTED, null, clock.instant(), clock.instant());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            logFailure(delivery, exception);
            repository.finish(delivery, Delivery.State.FAILED,
                    "Transport configuration is unavailable or invalid", clock.instant(), clock.instant());
        } catch (RuntimeException exception) {
            logFailure(delivery, exception);
            retryOrFail(delivery);
        }
    }

    private void send(Delivery delivery) {
        if (delivery.route().transport() == DestinationConfig.Transport.SMTP) {
            smtp.send(delivery);
        } else {
            snmp.send(delivery);
        }
    }

    private void retryOrFail(Delivery delivery) {
        boolean retry = delivery.attempts() < 5;

        // Attempts include the current claim: the four remaining waits are 30, 60, 120 and 240 seconds.
        long delaySeconds = retry ? 30L << (delivery.attempts() - 1) : 0;

        repository.finish(delivery, retry ? Delivery.State.PENDING : Delivery.State.FAILED,
                "Transport failed; acceptance may be uncertain", clock.instant(), clock.instant().plusSeconds(delaySeconds));
    }

    private void logFailure(Delivery delivery, RuntimeException exception) {
        // Transport exception messages may contain credentials or server responses; never persist or log them.
        LOG.warn("Notification delivery={} attempt={} failure={}",
                delivery.id(), delivery.attempts(), exception.getClass().getSimpleName());
    }

    @Scheduled(fixedDelay = 86_400_000, initialDelay = 60_000, scheduler = "notificationScheduler")
    public void prune() {
        repository.prune(clock.instant().minus(properties.retentionDays(), ChronoUnit.DAYS));
    }
}
