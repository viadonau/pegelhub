package at.pegelhub.connector.livetest;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

enum Scenario {
    FTP,
    TSTP,
    IEC,
    ICC;

    static Set<Scenario> parse(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("all")) {
            return EnumSet.allOf(Scenario.class);
        }
        EnumSet<Scenario> scenarios = EnumSet.noneOf(Scenario.class);
        for (String part : value.split(",")) {
            scenarios.add(Scenario.valueOf(part.trim().toUpperCase(Locale.ROOT)));
        }
        return scenarios;
    }
}
