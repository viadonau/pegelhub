package at.pegelhub.timeseries.api;

import at.pegelhub.timeseries.domain.SourceRepresentation;

import java.util.List;

public record ObservedPropertyResponse(String code, String canonicalUnit, List<SourceRepresentation> sourceRepresentations) { }
