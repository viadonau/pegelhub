package at.pegelhub.notifications.api;

import at.pegelhub.notifications.application.CredentialCatalog;
import at.pegelhub.notifications.application.NotificationProperties;
import at.pegelhub.notifications.application.Notifications;
import at.pegelhub.notifications.domain.Delivery;
import at.pegelhub.notifications.domain.Destination;
import at.pegelhub.notifications.domain.DestinationConfig;
import at.pegelhub.shared.api.ConfigurationYaml;
import at.pegelhub.shared.api.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** HTTP boundary; SecurityConfiguration separates authorized client submissions from administrator operations. */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final Notifications notifications;
    private final CredentialCatalog credentials;
    private final NotificationProperties properties;
    private final ConfigurationYaml yaml;

    public NotificationController(
            Notifications notifications,
            CredentialCatalog credentials,
            NotificationProperties properties,
            ConfigurationYaml yaml) {
        this.notifications = notifications;
        this.credentials = credentials;
        this.properties = properties;
        this.yaml = yaml;
    }

    public record Submission(List<UUID> destinations, String subject, String body) { }

    /** Correlates the queued per-destination deliveries, not a receipt from any recipient. */
    public record Accepted(UUID requestId) { }

    public record Status(boolean enabled, int retentionDays, List<CredentialCatalog.Reference> credentials) { }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Accepted submit(@RequestBody Submission request) {
        return new Accepted(notifications.submit(request.destinations(), request.subject(), request.body()));
    }

    @GetMapping("/status")
    public Status status() {
        return new Status(properties.enabled(), properties.retentionDays(), credentials.references());
    }

    @GetMapping("/destinations")
    public List<Destination> destinations() {
        return notifications.destinations();
    }

    @GetMapping("/destinations/{id}")
    public Destination destination(@PathVariable UUID id) {
        return notifications.destination(id);
    }

    @PostMapping("/destinations")
    @ResponseStatus(HttpStatus.CREATED)
    public Destination create(@RequestBody DestinationConfig config) {
        return notifications.create(config);
    }

    @PutMapping("/destinations/{id}")
    public Destination update(@PathVariable UUID id, @RequestBody DestinationConfig config) {
        return notifications.update(id, config);
    }

    @GetMapping("/deliveries")
    public Page<Delivery> deliveries(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit) {
        return notifications.history(offset, limit);
    }

    @GetMapping("/deliveries/{id}")
    public Delivery delivery(@PathVariable UUID id) {
        return notifications.delivery(id);
    }

    @GetMapping(value = "/destinations/{id}/export", produces = "application/yaml")
    public String export(@PathVariable UUID id) {
        return yaml.write(notifications.destination(id).configuration());
    }

    @PostMapping(value = "/destinations/import", consumes = {"application/yaml", "text/plain"})
    @ResponseStatus(HttpStatus.CREATED)
    public Destination importConfig(@RequestBody String document) {
        return notifications.create(yaml.read(document, DestinationConfig.class));
    }
}
