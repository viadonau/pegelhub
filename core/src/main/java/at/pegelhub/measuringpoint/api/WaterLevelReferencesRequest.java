package at.pegelhub.measuringpoint.api;

import java.math.BigDecimal;

public record WaterLevelReferencesRequest(
        int referenceSetYear,
        BigDecimal rnwCm,
        BigDecimal mwCm,
        BigDecimal hswCm,
        BigDecimal hw100Cm) { }
