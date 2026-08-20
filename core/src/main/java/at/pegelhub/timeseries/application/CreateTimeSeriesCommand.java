package at.pegelhub.timeseries.application;

import at.pegelhub.measuringpoint.domain.MeasuringPointId;
import at.pegelhub.shared.metadata.MetadataStatus;
import at.pegelhub.timeseries.domain.ObservedPropertyCode;
import at.pegelhub.timeseries.domain.SourceAssignment;

public record CreateTimeSeriesCommand(
        MeasuringPointId measuringPointId,
        ObservedPropertyCode observedProperty,
        MetadataStatus status,
        SourceAssignment sourceAssignment) { }
