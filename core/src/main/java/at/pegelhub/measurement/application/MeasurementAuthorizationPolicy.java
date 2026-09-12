package at.pegelhub.measurement.application;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.timeseries.domain.TimeSeriesId;

import java.util.Collection;

public interface MeasurementAuthorizationPolicy {

    /**
     * Checks the current actor's write role and active connector before loading target metadata.
     */
    ConnectorId requireWriter();

    /** Checks resource access for a writer already resolved by {@link #requireWriter()}. */
    void requireWrite(ConnectorId connectorId, MeasurementWriteTarget target);

    void requireRead(TimeSeriesId timeSeriesId);

    void requireReadBatch(Collection<TimeSeriesId> timeSeriesIds);
}
