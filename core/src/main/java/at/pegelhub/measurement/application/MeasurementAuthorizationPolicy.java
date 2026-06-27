package at.pegelhub.measurement.application;

import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.timeseries.domain.TimeSeriesId;

import java.util.Collection;

public interface MeasurementAuthorizationPolicy {

    ConnectorId requireWrite(TimeSeriesId timeSeriesId);

    ConnectorId requireWriteBatch(Collection<TimeSeriesId> timeSeriesIds);

    void requireRead(TimeSeriesId timeSeriesId);
}
