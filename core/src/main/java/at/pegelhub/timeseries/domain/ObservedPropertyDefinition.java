package at.pegelhub.timeseries.domain;

import java.util.List;

public record ObservedPropertyDefinition(
        String code,
        String canonicalUnit,
        List<SourceRepresentation> sourceRepresentations) {

    public ObservedPropertyDefinition {
        sourceRepresentations = List.copyOf(sourceRepresentations);
    }
}
