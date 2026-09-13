package at.pegelhub.measurement.persistence;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.measurement.domain.InternalProducerId;
import at.pegelhub.measurement.domain.Measurement;
import at.pegelhub.timeseries.domain.TimeSeriesId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class MeasurementOriginTest {
    @Test
    void connectorTagSetRemainsUnchangedAndInternalOriginIsExclusive() {
        var mapper = new InfluxMeasurementPointMapper();
        var series = new TimeSeriesId(UUID.randomUUID());
        var connector = new ConnectorId(UUID.randomUUID());
        var producer = new InternalProducerId(UUID.randomUUID());
        var time = Instant.parse("2026-09-13T08:00:00Z");

        String raw = mapper.toPoint(new Measurement(series, time, time, 4, connector)).toLineProtocol();
        assertThat(raw).startsWith(series.value() + ",submittedByConnectorId=" + connector.value() + " ")
                .doesNotContain("submittedByInternalProducerId");

        String derived = mapper.toPoint(new Measurement(series, time, time, 4, null, producer)).toLineProtocol();
        assertThat(derived).startsWith(series.value() + ",submittedByInternalProducerId=" + producer.value() + " ")
                .doesNotContain("submittedByConnectorId");

        assertThatThrownBy(() -> new Measurement(series, time, time, 4, connector, producer)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Measurement(series, time, time, 4, null, null)).isInstanceOf(IllegalArgumentException.class);
    }
}
