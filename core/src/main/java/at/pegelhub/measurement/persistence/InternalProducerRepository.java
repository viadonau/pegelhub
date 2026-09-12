package at.pegelhub.measurement.persistence;

import at.pegelhub.measurement.domain.InternalProducerId;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
public class InternalProducerRepository {

    private final JdbcTemplate jdbc;

    public InternalProducerRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The caller holds this transaction-scoped graph lock from dependency validation through binding replacement. */
    public void lockConfiguration() {
        jdbc.execute("lock table internal_producer in share row exclusive mode");
    }

    public void register(InternalProducerId id) {
        jdbc.update("insert into internal_producer(id) values (?) on conflict do nothing", id.value());
    }

    public List<TimeSeriesId> inputs(InternalProducerId id) {
        return jdbc.query("select time_series_id from internal_producer_input where producer_id = ? order by time_series_id",
                (rs, row) -> new TimeSeriesId(rs.getObject(1, UUID.class)), id.value());
    }

    public TimeSeriesId output(InternalProducerId id) {
        return jdbc.query("select output_time_series_id from internal_producer where id = ?",
                        (rs, row) -> rs.getObject(1, UUID.class), id.value())
                .stream()
                .filter(Objects::nonNull)
                .findFirst()
                .map(TimeSeriesId::new)
                .orElse(null);
    }

    public void configure(InternalProducerId id, List<TimeSeriesId> inputs, TimeSeriesId output) {
        jdbc.update("delete from internal_producer_input where producer_id = ?", id.value());

        for (var input : inputs) {
            jdbc.update("insert into internal_producer_input values (?, ?)", id.value(), input.value());
        }

        jdbc.update("update internal_producer set output_time_series_id = ? where id = ?",
                output == null ? null : output.value(), id.value());
    }
}
