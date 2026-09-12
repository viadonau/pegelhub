package at.pegelhub.quality.persistence;

import at.pegelhub.quality.domain.Finding;
import at.pegelhub.quality.domain.ProfileConfig;
import at.pegelhub.quality.domain.QualityProfile;
import at.pegelhub.quality.domain.QualityRun;
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
 * Profile/run persistence, private to quality. The worker provides the global single-process slot;
 * row locks protect a profile from concurrent edits but are not a distributed QA scheduler.
 */
@Repository
public class QualityRepository {

    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public QualityRepository(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public List<QualityProfile> profiles() {
        return jdbc.query("select * from quality_profile order by configuration->>'name', id", this::profile);
    }

    /** With {@code lock=true}, the caller must keep its edit transaction open while checking the active run. */
    public QualityProfile profile(UUID id, boolean lock) {
        return jdbc.query("select * from quality_profile where id = ?" + (lock ? " for update" : ""), this::profile, id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("QA profile not found"));
    }

    public void save(UUID id, ProfileConfig config, Instant nextRunAt) {
        jdbc.update("""
                insert into quality_profile(id, configuration, enabled, next_run_at) values (?, ?::jsonb, ?, ?)
                on conflict(id) do update set configuration = excluded.configuration, enabled = excluded.enabled,
                    next_run_at = excluded.next_run_at, run_requested = false
                """, id, json.writeValueAsString(config), config.enabled(), Timestamp.from(nextRunAt));
    }

    public void requestRun(UUID id) {
        jdbc.update("update quality_profile set run_requested = true where id = ?", id);
    }

    /** Claims a due profile and snapshots its configuration in the same transaction, or returns null if none is due. */
    @Transactional
    public QualityRun claim(Instant now) {
        var candidates = jdbc.query("""
                select * from quality_profile where enabled and current_run_id is null and (run_requested or next_run_at <= ?)
                order by next_run_at, id limit 1 for update skip locked
                """, this::profile, Timestamp.from(now));
        if (candidates.isEmpty()) {
            return null;
        }

        var profile = candidates.getFirst();
        UUID id = UUID.randomUUID();

        jdbc.update("""
                insert into quality_run(id, profile_id, configuration, state, started_at, output_state)
                values (?, ?, ?::jsonb, 'RUNNING', ?, ?)
                """, id, profile.id(), json.writeValueAsString(profile.configuration()), Timestamp.from(now),
                profile.configuration().output() == null ? "NOT_CONFIGURED" : "SUPPRESSED");
        jdbc.update("update quality_profile set current_run_id = ?, run_requested = false where id = ?", id, profile.id());

        return run(id);
    }

    public QualityRun run(UUID id) {
        return jdbc.query("select * from quality_run where id = ?", this::mapRun, id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("QA run not found"));
    }

    /** Joins the caller's transaction so findings, terminal run state and notification submissions commit together. */
    public void finish(
            UUID id,
            QualityRun.State state,
            List<Finding> findings,
            QualityRun.OutputState output,
            String error,
            Instant now) {
        jdbc.batchUpdate("insert into quality_finding(run_id, finding) values (?, ?::jsonb)", findings, 500,
                (statement, finding) -> {
                    statement.setObject(1, id);
                    statement.setString(2, json.writeValueAsString(finding));
                });

        jdbc.update("""
                update quality_run set state = ?, completed_at = ?, finding_count = ?, output_state = ?, error = ? where id = ?
                """, state.name(), Timestamp.from(now), findings.size(), output.name(), error, id);
    }

    public void output(UUID run, QualityRun.OutputState state, String error) {
        jdbc.update("update quality_run set output_state = ?, error = ? where id = ?", state.name(), error, run);
    }

    public void release(QualityRun run, Instant now) {
        // Fixed delay is measured from completion; missed intervals are not replayed.
        jdbc.update("""
                update quality_profile set current_run_id = null, next_run_at = ? where id = ? and current_run_id = ?
                """, Timestamp.from(now.plusSeconds(run.configuration().intervalSeconds())), run.profileId(), run.id());
    }

    /**
     * Reconciles abandoned work only while this Core instance's QA worker is idle.
     * Preserves committed findings and marks uncertain Influx output separately rather than resending it.
     * Calling this while another instance executes QA would interrupt live work.
     */
    @Transactional
    public void recover(Instant now) {
        jdbc.update("""
                update quality_run set state = 'INTERRUPTED', completed_at = ?, error = 'Run interrupted by lifecycle recovery'
                where state = 'RUNNING'
                """, Timestamp.from(now));

        jdbc.update("update quality_run set output_state = 'INTERRUPTED', "
                + "error = 'Output acceptance uncertain after lifecycle recovery' "
                + "where output_state = 'PENDING'");

        jdbc.update("""
                update quality_profile set current_run_id = null, run_requested = false,
                    next_run_at = ?::timestamptz + ((configuration->>'intervalSeconds')::int * interval '1 second') where current_run_id is not null
                """, Timestamp.from(now));
    }

    public Page<QualityRun> runs(UUID profileId, int offset, int limit) {
        Page.validate(offset, limit);

        String where = profileId == null ? "" : " where profile_id = ?";
        Object[] page = profileId == null ? new Object[]{limit, offset} : new Object[]{profileId, limit, offset};
        Object[] count = profileId == null ? new Object[]{} : new Object[]{profileId};

        return new Page<>(
                jdbc.query("select * from quality_run" + where + " order by started_at desc, id limit ? offset ?",
                        this::mapRun, page),
                offset, limit,
                jdbc.queryForObject("select count(*) from quality_run" + where, Long.class, count));
    }

    public Page<Finding> findings(UUID runId, int offset, int limit) {
        Page.validate(offset, limit);
        run(runId);

        return new Page<>(
                jdbc.query("select finding from quality_finding where run_id = ? order by id limit ? offset ?",
                        (rs, row) -> json.readValue(rs.getString(1), Finding.class), runId, limit, offset),
                offset, limit,
                jdbc.queryForObject("select count(*) from quality_finding where run_id = ?", Long.class, runId));
    }

    /** Keyset page of terminal candidates; the caller must still retain runs referenced by delivery history. */
    public List<UUID> expired(Instant before, UUID after) {
        return jdbc.query("""
                select id from quality_run where completed_at < ? and state <> 'RUNNING' and output_state <> 'PENDING'
                and (?::uuid is null or id > ?::uuid) order by id limit 1000
                """, (rs, row) -> rs.getObject(1, UUID.class), Timestamp.from(before), after, after);
    }

    public void deleteRun(UUID id) {
        jdbc.update("delete from quality_run where id = ?", id);
    }

    private QualityProfile profile(ResultSet rs, int row) throws SQLException {
        return new QualityProfile(
                rs.getObject("id", UUID.class),
                json.readValue(rs.getString("configuration"), ProfileConfig.class),
                rs.getTimestamp("next_run_at").toInstant(),
                rs.getObject("current_run_id", UUID.class),
                rs.getBoolean("run_requested"));
    }

    private QualityRun mapRun(ResultSet rs, int row) throws SQLException {
        return new QualityRun(
                rs.getObject("id", UUID.class),
                rs.getObject("profile_id", UUID.class),
                json.readValue(rs.getString("configuration"), ProfileConfig.class),
                QualityRun.State.valueOf(rs.getString("state")),
                rs.getTimestamp("started_at").toInstant(),
                rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toInstant(),
                rs.getInt("finding_count"),
                QualityRun.OutputState.valueOf(rs.getString("output_state")),
                rs.getString("error"));
    }
}
