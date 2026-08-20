package at.pegelhub.measuringpoint.domain;

import java.math.BigDecimal;

public record WaterLevelReferences(
        int referenceSetYear,
        BigDecimal rnwCm,
        BigDecimal mwCm,
        BigDecimal hswCm,
        BigDecimal hw100Cm) {

    public WaterLevelReferences {
        if (referenceSetYear < 1 || referenceSetYear > 9999) {
            throw new IllegalArgumentException("referenceSetYear must be a valid calendar year");
        }
        if (rnwCm == null && mwCm == null && hswCm == null && hw100Cm == null) {
            throw new IllegalArgumentException("water level references need at least one value");
        }
    }
}
