package at.pegelhub.connector.livetest;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

enum Scenario {
    FTP,
    TSTP,
    IEC,
    ICC;

    static final String VALID_VALUES = "all, ftp, tstp, iec, icc";

    static Set<Scenario> parse(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("all")) {
            return EnumSet.allOf(Scenario.class);
        }
        EnumSet<Scenario> scenarios = EnumSet.noneOf(Scenario.class);
        for (String part : value.split(",")) {
            String scenario = part.trim();
            if (scenario.isBlank()) {
                continue;
            }
            try {
                scenarios.add(Scenario.valueOf(scenario.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Unknown live connector scenario '" + scenario + "'. Expected one of: " + VALID_VALUES, e);
            }
        }
        if (scenarios.isEmpty()) {
            throw new IllegalArgumentException("No live connector scenario selected. Expected one of: " + VALID_VALUES);
        }
        return scenarios;
    }
}
