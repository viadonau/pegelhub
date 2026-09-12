package at.pegelhub.notifications.application;

import at.pegelhub.notifications.domain.Delivery;
import at.pegelhub.notifications.domain.Destination;
import at.pegelhub.notifications.domain.DestinationConfig;
import at.pegelhub.notifications.persistence.NotificationRepository;
import at.pegelhub.shared.api.Page;
import at.pegelhub.shared.error.MetadataConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Durable submission interface: success means queued in PostgreSQL, never delivered to a recipient.
 * Submission callers select destination IDs, never transport credentials or arbitrary hosts.
 * Configuration edits affect future submissions only, except disabling cancels pending deliveries.
 */
@Service
public class Notifications {

    private final NotificationRepository repository;
    private final CredentialCatalog credentials;
    private final Clock clock;

    public Notifications(NotificationRepository repository, CredentialCatalog credentials, Clock clock) {
        this.repository = repository;
        this.credentials = credentials;
        this.clock = clock;
    }

    public List<Destination> destinations() {
        return repository.destinations();
    }

    public Destination destination(UUID id) {
        return repository.destination(id, false);
    }

    public Page<Delivery> history(int offset, int limit) {
        return repository.history(offset, limit);
    }

    public Delivery delivery(UUID id) {
        return repository.get(id);
    }

    /** Retain source history until all referencing deliveries, including completed history, are purged. */
    public boolean retainsSource(UUID source) {
        return repository.references(source);
    }

    @Transactional
    public Destination create(DestinationConfig configuration) {
        credentials.requireReference(configuration);

        return save(UUID.randomUUID(), configuration);
    }

    @Transactional
    public Destination update(UUID id, DestinationConfig configuration) {
        var existing = repository.destination(id, true);

        // Credential removal must not make an existing destination impossible to disable.
        if (configuration.enabled() || !configuration.credentialRef().equals(existing.configuration().credentialRef())) {
            credentials.requireReference(configuration);
        }

        return save(id, configuration);
    }

    private Destination save(UUID id, DestinationConfig configuration) {
        var destination = new Destination(id, configuration);
        repository.save(destination);

        if (!configuration.enabled()) {
            repository.cancel(id, clock.instant());
        }

        return destination;
    }

    public void requireDestinations(List<UUID> ids) {
        if (ids == null || ids.size() > 100 || ids.stream().distinct().count() != ids.size()) {
            throw new IllegalArgumentException("Invalid destination selection");
        }

        ids.forEach(id -> repository.destination(id, false));
    }

    /** Queues one delivery per enabled destination or rolls back the complete submission on rejection. */
    @Transactional
    public UUID submit(List<UUID> destinations, String subject, String body) {
        if (destinations == null || destinations.isEmpty()) {
            throw new IllegalArgumentException("Select a destination");
        }

        return enqueue(null, destinations, subject, body);
    }

    /**
     * Joins the caller's PostgreSQL transaction without network I/O. Disabled destinations are
     * skipped so an operator's pause does not prevent a QA result from committing. The run ID
     * prevents duplicate submissions within that run; later runs intentionally notify again.
     */
    @Transactional
    public UUID submitForRun(UUID runId, List<UUID> destinations, String subject, String body) {
        return enqueue(Objects.requireNonNull(runId), destinations, subject, body);
    }

    private UUID enqueue(UUID source, List<UUID> destinations, String subject, String body) {
        DestinationConfig.text(subject, 256, "subject");
        if (body == null || body.isBlank() || body.length() > 8192) {
            throw new IllegalArgumentException("Invalid message body");
        }
        requireDestinations(destinations);

        UUID request = UUID.randomUUID();

        // Lock in a stable order across submissions, and hold the lock until routing is snapshotted.
        for (var id : destinations.stream().sorted().toList()) {
            var destination = repository.destination(id, true);
            if (!destination.configuration().enabled()) {
                if (source != null) {
                    continue;
                }
                throw new MetadataConflictException("Destination is disabled");
            }

            repository.enqueue(request, source, destination, subject, body, clock.instant());
        }

        return request;
    }
}
