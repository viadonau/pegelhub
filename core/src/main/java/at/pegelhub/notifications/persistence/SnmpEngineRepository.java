package at.pegelhub.notifications.persistence;

import org.snmp4j.mp.MPv3;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class SnmpEngineRepository {

    private final JdbcTemplate jdbc;

    public SnmpEngineRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Engine(byte[] id, int boots) { }

    /**
     * Retains the authoritative engine ID and commits a new boot count before this process sends v3 traps.
     * The transport calls this once per process, lazily so unused SNMP cannot affect startup.
     */
    @Transactional
    public Engine boot() {
        return jdbc.queryForObject("""
                insert into notification_snmp_engine(id, engine_id, boots) values (1, ?, 1)
                on conflict(id) do update set boots = notification_snmp_engine.boots + 1
                returning engine_id, boots
                """, (rs, row) -> new Engine(rs.getBytes(1), rs.getInt(2)), MPv3.createLocalEngineID());
    }
}
