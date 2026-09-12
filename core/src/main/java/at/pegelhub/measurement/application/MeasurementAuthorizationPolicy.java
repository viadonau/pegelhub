package at.pegelhub.measurement.application;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.timeseries.domain.TimeSeriesId;

import java.util.Collection;

public interface MeasurementAuthorizationPolicy {

    /**
     * Checks that the current actor can write as an active connector. Call this before loading target metadata.
     */
    ConnectorId requireWriter();

    /**
     * Checks that the series, measuring point and station are active, and that this connector is the source.
     * Pass the connector returned by {@link #requireWriter()}; this method does not authenticate the caller.
     * It also does not check whether the values can be converted.
     */
    void requireWrite(ConnectorId connectorId, MeasurementWriteTarget target);

    void requireRead(TimeSeriesId timeSeriesId);

    void requireReadBatch(Collection<TimeSeriesId> timeSeriesIds);
}
