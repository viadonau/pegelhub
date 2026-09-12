package at.pegelhub.timeseries.api;

import at.pegelhub.timeseries.domain.MeasurementRepresentation;

import java.util.List;

public record ObservedPropertyResponse(String code, String canonicalUnit, List<MeasurementRepresentation> sourceRepresentations) { }
