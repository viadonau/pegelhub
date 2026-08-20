package at.pegelhub.timeseries.application;

import at.pegelhub.shared.metadata.MetadataStatus;
import at.pegelhub.timeseries.domain.SourceAssignment;

public record UpdateTimeSeriesCommand(MetadataStatus status, SourceAssignment sourceAssignment) { }
