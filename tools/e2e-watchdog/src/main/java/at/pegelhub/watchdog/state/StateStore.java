package at.pegelhub.watchdog.state;

import at.pegelhub.watchdog.Json;
import at.pegelhub.watchdog.config.WatchdogConfig;
import at.pegelhub.watchdog.monitoring.CheckState;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Durable watchdog state owned by the single monitoring loop. Transactions make multi-document updates atomic.
 * SQLite and file ownership are handled by {@link SqliteStateStorage}. HTTP and SNMP stay outside this store.
 *
 * <p>Each key below is one row in {@code watchdog.db}'s {@code document(key, body)} table;
 * {@code body} is JSON, not a separate SQL column for each field. Rows hold current state, not a history:
 * <ul>
 *   <li>{@code identity}: route/receiver binding from {@link WatchdogConfig#identity()}, checked on reopen.</li>
 *   <li>{@code check}: latest evaluation, including status, reason and measurement timestamp/age.</li>
 *   <li>{@code clockWatermark}: greatest accepted evaluation time, retained to detect clock rollback.</li>
 *   <li>{@code progress}: session start or last persisted completed-loop heartbeat, used for container health.</li>
 *   <li>{@code engine}: public SNMPv3 engine ID bytes and boot count, committed before sending.</li>
 *   <li>{@code publication.HOST:PORT}: last locally emitted ERR/OK and any subsequent send failure,
 *       independently for each receiver. This is not delivery acknowledgement.</li>
 * </ul>
 *
 * <p>Example after an ERR emission (fictional values, identity abbreviated). This groups keys and their
 * decoded JSON bodies for readability; the outer object is not itself stored:
 * <pre>{@code
 * {
 *   "identity": "return-freshness-v2|...",
 *   "check": {
 *     "status": "CRITICAL", "reason": "stale",
 *     "evaluatedAt": "2026-09-20T12:10:01Z", "evidenceAt": "2026-09-20T12:00:00Z",
 *     "ageSeconds": 601.0
 *   },
 *   "clockWatermark": "2026-09-20T12:10:01Z",
 *   "progress": "2026-09-20T12:10:02Z",
 *   "engine": {"id": [1, 2, 3, 4, 5], "boots": 2},
 *   "publication.receiver-a.example.test:162": {
 *     "signal": "ERR", "emittedAt": "2026-09-20T12:10:02Z", "failed": false
 *   }
 * }
 * }</pre>
 *
 * <p>Rows are created when first needed; {@code engine} is created for SNMPv3 and publication rows on
 * send attempts. Null fields are omitted by the JSON codec: an initial failed send stores only
 * {@code {"failed":true}}. Engine ID bytes are a numeric array on disk, not the hex string shown by
 * {@code status}. Diagnostics also omit {@code identity} and derive {@code healthy}/{@code publisherError};
 * those flags are not stored rows. Credentials, measurement values, retry deadlines and network sessions
 * are not persisted here.
 */
public final class StateStore implements AutoCloseable {
    private static final long LOOP_STALE_AFTER_SECONDS = 180;
    private static final long DIAGNOSTIC_CLOCK_TOLERANCE_SECONDS = 5;

    private final SqliteStateStorage storage;

    public record Engine(byte[] id, int boots) {
    }

    /**
     * Last successful local emission, not recipient acknowledgement. Signal and time remain null
     * if sending has only ever failed; that failure must survive even without an emitted alarm.
     */
    public record Publication(String signal, Instant emittedAt, boolean failed) {
    }

    public StateStore(Path directory, WatchdogConfig config) throws IOException {
        storage = new SqliteStateStorage(directory);
        try {
            bindIdentity(config);
        } catch (Exception failure) {
            try {
                storage.close();
            } catch (Exception cleanup) {
                failure.addSuppressed(cleanup);
            }
            throw new IOException("Cannot initialize watchdog state (identity or database failure)", failure);
        }
    }

    /** Only a new database may lack identity; existing alarms and engine state belong to their original route. */
    private void bindIdentity(WatchdogConfig config) {
        String previous = storage.get("identity", String.class);
        if (storage.existedOnOpen() && !config.identity().equals(previous)) {
            throw new IllegalStateException("Route identity changed");
        }
        if (!storage.existedOnOpen()) {
            storage.put("identity", config.identity());
        }
    }

    /** Forget the old observation, not emitted alarms: recovery requires a successful read in this session. */
    public void beginSession(Instant now) {
        storage.transaction(() -> {
            storage.put("check", CheckState.unknown("starting", now));
            storage.put("progress", now);
            // Replace only the old session's worker heartbeats, never its alarms or SNMP engine.
            storage.remove("progress.freshness");
            storage.remove("progress.publisher");
        });
    }

    public CheckState check() {
        return storage.get("check", CheckState.class);
    }

    /** Commit observations before publication, without holding a transaction during an HTTP or SNMP operation. */
    public void record(CheckState state) {
        storage.transaction(() -> {
            Instant watermark = storage.get("clockWatermark", Instant.class);
            if (watermark != null && state.evaluatedAt().isBefore(watermark)) {
                // A backwards clock must not make a previously stale timestamp healthy again.
                storage.put("check", CheckState.unknown("clock_regressed", state.evaluatedAt()));
            } else {
                storage.put("clockWatermark", state.evaluatedAt());
                storage.put("check", state);
            }
        });
    }

    public void progress(Instant now) {
        storage.put("progress", now);
    }

    /** The trap sender is authoritative. Commit each boot increment before any packet uses that engine. */
    public Engine bootEngine(byte[] candidate) {
        return storage.transaction(() -> {
            Engine previous = storage.get("engine", Engine.class);
            if (previous != null && previous.boots() >= Integer.MAX_VALUE - 1) {
                throw new IllegalStateException("SNMP engine exhausted");
            }

            byte[] engineId = previous == null ? candidate : previous.id();
            int boots = previous == null ? 1 : previous.boots() + 1;
            Engine next = new Engine(engineId, boots);
            storage.put("engine", next);
            return next;
        });
    }

    public Publication publication(String receiver) {
        return storage.get("publication." + receiver, Publication.class);
    }

    public void emitted(String receiver, String signal, Instant now) {
        storage.put("publication." + receiver, new Publication(signal, now, false));
    }

    /** Coalescing a route event does not repair its receiver. Only a later successful emission clears this. */
    public void emissionFailed(String receiver) {
        Publication previous = publication(receiver);
        storage.put("publication." + receiver, new Publication(
                previous == null ? null : previous.signal(),
                previous == null ? null : previous.emittedAt(),
                true));
    }

    @Override
    public void close() throws IOException {
        storage.close();
    }

    /** Read-only diagnostics never create storage. The engine ID is public; no credentials are stored here. */
    public static Map<String, Object> inspect(Path directory, Instant now) throws Exception {
        var snapshot = SqliteStateStorage.inspect(directory);
        var documents = diagnosticDocuments(snapshot.documents());
        boolean publisherFailed = hasPublicationFailure(documents);
        documents.put("publisherError", publisherFailed);

        // Container health measures supervision, not whether the monitored route is currently healthy.
        boolean healthy = snapshot.owned() && loopIsCurrent(documents, now) && !publisherFailed;
        return Map.of("schemaVersion", snapshot.schemaVersion(), "healthy", healthy, "state", documents);
    }

    private static boolean hasPublicationFailure(Map<String, Object> documents) {
        return documents.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("publication."))
                .anyMatch(entry -> entry.getValue() instanceof Map<?, ?> publication
                        && Boolean.TRUE.equals(publication.get("failed")));
    }

    private static Map<String, Object> diagnosticDocuments(Map<String, String> storedDocuments) {
        var documents = new LinkedHashMap<String, Object>();
        for (var row : storedDocuments.entrySet()) {
            String key = row.getKey();
            String body = row.getValue();
            if (key.equals("identity")) {
                continue;
            }
            if (key.equals("engine")) {
                var engine = Json.CODEC.fromJson(body, Engine.class);
                documents.put("snmpEngine", Map.of(
                        "id", HexFormat.of().formatHex(engine.id()), "boots", engine.boots()));
            } else {
                documents.put(key, Json.CODEC.fromJson(body, Object.class));
            }
        }
        return documents;
    }

    private static boolean loopIsCurrent(Map<String, Object> documents, Instant now) {
        if (!(documents.get("progress") instanceof String time)) {
            return false;
        }
        Instant progress = Instant.parse(time);
        // The loop can advance after the CLI captures now but before it opens its read transaction.
        return !progress.isAfter(now.plusSeconds(DIAGNOSTIC_CLOCK_TOLERANCE_SECONDS))
                && progress.isAfter(now.minusSeconds(LOOP_STALE_AFTER_SECONDS));
    }
}
