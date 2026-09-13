package at.pegelhub.notifications.persistence;

import at.pegelhub.notifications.domain.Delivery;
import at.pegelhub.notifications.domain.Destination;
import at.pegelhub.notifications.domain.DestinationConfig;
import at.pegelhub.shared.api.Page;
import at.pegelhub.shared.error.NotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Queue persistence, private to notifications. Destination changes and enqueue operations join
 * the application transaction; claim and finish own short transactions with no transport I/O.
 */
@Repository
public class NotificationRepository {

    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public NotificationRepository(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public List<Destination> destinations() {
        return jdbc.query("select * from notification_destination order by configuration->>'name', id", this::destination);
    }

    /** With {@code lock=true}, the caller must hold a transaction through its configuration change or enqueue. */
    public Destination destination(UUID id, boolean lock) {
        return jdbc.query("select * from notification_destination where id = ?" + (lock ? " for update" : ""),
                        this::destination, id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Destination not found"));
    }

    public void save(Destination destination) {
        jdbc.update("""
                insert into notification_destination(id, configuration, enabled) values (?, ?::jsonb, ?)
                on conflict(id) do update set configuration = excluded.configuration, enabled = excluded.enabled
                """, destination.id(), json.writeValueAsString(destination.configuration()), destination.configuration().enabled());
    }

    public void cancel(UUID destination, Instant now) {
        // Even an expired SENDING lease may still be in network I/O. Only the sender may settle that attempt.
        jdbc.update("""
                update notification_delivery set state = 'CANCELLED', completed_at = ?, lease_token = null, lease_until = null
                where destination_id = ? and state = 'PENDING'
                """, Timestamp.from(now), destination);
    }

    public void enqueue(
            UUID request,
            UUID source,
            Destination destination,
            String subject,
            String body,
            Instant now) {
        // QA uses the run ID as source: retries deduplicate within a run, never across successive runs.
        jdbc.update("""
                insert into notification_delivery(id, request_id, source_id, destination_id, route, subject, body,
                    state, attempts, created_at, next_attempt_at)
                values (?, ?, ?, ?, ?::jsonb, ?, ?, 'PENDING', 0, ?, ?)
                on conflict(source_id, destination_id) do nothing
                """, UUID.randomUUID(), request, source, destination.id(), json.writeValueAsString(destination.configuration()),
                subject, body, Timestamp.from(now), Timestamp.from(now));
    }

    /**
     * Claims one due delivery or returns null. Attempts count claims, including ones interrupted before sending.
     * Expired leases are recoverable work, not evidence that a receiver rejected an earlier attempt.
     */
    @Transactional
    public Delivery claim(Instant now) {
        jdbc.update("""
                update notification_delivery set state = 'FAILED', completed_at = ?, last_error = 'Attempt limit reached after interruption',
                    lease_token = null, lease_until = null
                where state = 'SENDING' and lease_until <= ? and attempts >= 5
                """, Timestamp.from(now), Timestamp.from(now));

        var rows = jdbc.query("""
                select * from notification_delivery where attempts < 5 and
                ((state = 'PENDING' and next_attempt_at <= ?) or (state = 'SENDING' and lease_until <= ?))
                order by next_attempt_at, id limit 1 for update skip locked
                """, this::delivery, Timestamp.from(now), Timestamp.from(now));
        if (rows.isEmpty()) {
            return null;
        }

        var item = rows.getFirst();
        UUID token = UUID.randomUUID();

        jdbc.update("""
                update notification_delivery set state = 'SENDING', attempts = attempts + 1, lease_token = ?, lease_until = ?
                where id = ?
                """, token, Timestamp.from(now.plusSeconds(300)), item.id());

        return get(item.id());
    }

    public Delivery get(UUID id) {
        return jdbc.query("select * from notification_delivery where id = ?", this::delivery, id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Delivery not found"));
    }

    /**
     * Settles only the supplied claim token; a replaced worker cannot overwrite its successor.
     * This fences database updates, not network sends, and therefore does not guarantee exactly-once delivery.
     */
    @Transactional
    public void finish(
            Delivery delivery,
            Delivery.State state,
            String error,
            Instant now,
            Instant nextAttemptAt) {
        var destination = destination(delivery.destinationId(), true);

        // Serialize retry creation with disable/cancel so no new pending work escapes cancellation.
        if (state == Delivery.State.PENDING && !destination.configuration().enabled()) {
            state = Delivery.State.CANCELLED;
        }

        jdbc.update("""
                update notification_delivery set state = ?, last_error = ?, next_attempt_at = ?, completed_at = ?,
                    lease_token = null, lease_until = null where id = ? and state = 'SENDING' and lease_token = ?
                """, state.name(), error, Timestamp.from(nextAttemptAt),
                state == Delivery.State.PENDING ? null : Timestamp.from(now),
                delivery.id(), delivery.leaseToken());
    }

    public Page<Delivery> history(int offset, int limit) {
        Page.validate(offset, limit);

        return new Page<>(
                jdbc.query("select * from notification_delivery order by created_at desc, id limit ? offset ?",
                        this::delivery, limit, offset),
                offset, limit,
                jdbc.queryForObject("select count(*) from notification_delivery", Long.class));
    }

    public boolean references(UUID source) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "select exists(select 1 from notification_delivery where source_id = ?)", Boolean.class, source));
    }

    public void prune(Instant before) {
        jdbc.update("delete from notification_delivery where completed_at < ? and state in ('ACCEPTED','FAILED','CANCELLED')",
                Timestamp.from(before));
    }

    private Destination destination(ResultSet rs, int row) throws SQLException {
        return new Destination(
                rs.getObject("id", UUID.class),
                json.readValue(rs.getString("configuration"), DestinationConfig.class));
    }

    private Delivery delivery(ResultSet rs, int row) throws SQLException {
        return new Delivery(
                rs.getObject("id", UUID.class),
                rs.getObject("request_id", UUID.class),
                rs.getObject("source_id", UUID.class),
                rs.getObject("destination_id", UUID.class),
                json.readValue(rs.getString("route"), DestinationConfig.class),
                rs.getString("subject"),
                rs.getString("body"),
                Delivery.State.valueOf(rs.getString("state")),
                rs.getInt("attempts"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("next_attempt_at").toInstant(),
                rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toInstant(),
                rs.getString("last_error"),
                rs.getObject("lease_token", UUID.class));
    }
}
