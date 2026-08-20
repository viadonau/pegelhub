package at.pegelhub.timeseries.domain;

import java.util.List;
import java.util.Optional;

public final class ObservedPropertyCatalog {

    private static final List<ObservedPropertyDefinition> DEFINITIONS = List.of(
            new ObservedPropertyDefinition(
                    "water-level", "cm", List.of(SourceRepresentation.CANONICAL, SourceRepresentation.METRES_ABOVE_ADRIA)),
            new ObservedPropertyDefinition("water-temperature", "Cel", List.of(SourceRepresentation.CANONICAL)),
            new ObservedPropertyDefinition("discharge", "m3/s", List.of(SourceRepresentation.CANONICAL)));

    private ObservedPropertyCatalog() {
    }

    public static List<ObservedPropertyDefinition> definitions() {
        return DEFINITIONS;
    }

    public static Optional<ObservedPropertyDefinition> find(String code) {
        return DEFINITIONS.stream().filter(definition -> definition.code().equals(code)).findFirst();
    }

    public static boolean allows(String code, SourceRepresentation representation) {
        return find(code).map(definition -> definition.sourceRepresentations().contains(representation)).orElse(false);
    }
}
