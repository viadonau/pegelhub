package at.pegelhub.measurement.application;

import at.pegelhub.timeseries.domain.TimeSeriesId;

import java.util.Collection;

public interface MeasurementAuthorizationPolicy {

    MeasurementWriteAuthorization requireWrite(TimeSeriesId timeSeriesId);

    MeasurementWriteAuthorization requireWriteBatch(Collection<TimeSeriesId> timeSeriesIds);

    void requireRead(TimeSeriesId timeSeriesId);

    void requireReadBatch(Collection<TimeSeriesId> timeSeriesIds);
}
