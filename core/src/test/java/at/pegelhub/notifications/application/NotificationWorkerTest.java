package at.pegelhub.notifications.application;

import at.pegelhub.notifications.domain.Delivery;
import at.pegelhub.notifications.domain.Destination;
import at.pegelhub.notifications.domain.DestinationConfig;
import at.pegelhub.notifications.persistence.NotificationRepository;
import at.pegelhub.notifications.transport.SmtpDeliveryTransport;
import at.pegelhub.notifications.transport.SnmpDeliveryTransport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mail.MailSendException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationWorkerTest {
    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final SmtpDeliveryTransport smtp = mock(SmtpDeliveryTransport.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-13T10:00:00Z"), ZoneOffset.UTC);
    private final NotificationWorker worker = new NotificationWorker(new NotificationProperties(true, 90, Map.of()),
            repository, smtp, mock(SnmpDeliveryTransport.class), clock);

    @ParameterizedTest
    @CsvSource({"1,30,PENDING", "2,60,PENDING", "3,120,PENDING", "4,240,PENDING", "5,0,FAILED"})
    void retriesTheSameDeliveryWithBoundedDelays(int attempt, int delay, Delivery.State state) {
        var item = delivery(attempt);
        doThrow(new MailSendException("A transport message containing secrets must not be persisted")).when(smtp).send(item);

        worker.tick();

        verify(repository).finish(eq(item), eq(state), eq("Transport failed; acceptance may be uncertain"),
                eq(clock.instant()), eq(clock.instant().plusSeconds(delay)));
    }

    @Test
    void invalidConfigurationTerminatesWithoutRetry() {
        var item = delivery(1);
        doThrow(new IllegalStateException("missing credentials")).when(smtp).send(item);

        worker.tick();

        verify(repository).finish(eq(item), eq(Delivery.State.FAILED), eq("Transport configuration is unavailable or invalid"),
                eq(clock.instant()), eq(clock.instant()));
    }

    private Delivery delivery(int attempt) {
        var route = new DestinationConfig("SMTP", true, DestinationConfig.Transport.SMTP, "relay",
                new DestinationConfig.MailRoute("sender@example.test", List.of("recipient@example.test"), null), null);
        var item = new Delivery(UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(), route,
                "Subject", "Body", Delivery.State.SENDING, attempt, clock.instant(), clock.instant(), null, null, UUID.randomUUID());

        when(repository.claim(any())).thenReturn(item);
        when(repository.destination(item.destinationId(), false)).thenReturn(new Destination(item.destinationId(), route));

        return item;
    }
}
