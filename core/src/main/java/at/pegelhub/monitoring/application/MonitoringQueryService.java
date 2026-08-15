package at.pegelhub.monitoring.application;

import at.pegelhub.timeseries.domain.TimeSeriesId;

import java.time.Duration;

public interface MonitoringQueryService {

    MonitoringCollection readCollection(Duration latestWithin);

    MonitoringDetail readDetail(TimeSeriesId timeSeriesId, Duration latestWithin);
}
