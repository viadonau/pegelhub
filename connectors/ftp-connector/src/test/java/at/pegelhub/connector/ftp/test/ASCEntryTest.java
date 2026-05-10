package at.pegelhub.connector.ftp.test;

import at.pegelhub.connector.ftp.fileparsing.implementation.ASCEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class ASCEntryTest {
    private ASCEntry entry;

    @BeforeEach
    public void setup() {
        entry = new ASCEntry();
        entry.setParameter("Wasserstand");
        entry.setLocation("10001033");
        entry.setDefKind("M");
        entry.setOrigin("A");
        entry.setRowKind("Z");
        entry.setVersion("0");
        entry.setX("627153.4");
        entry.setY("483804.2");
        entry.setUnit("cm");
        entry.setAccuracy("0");
        entry.setTolerance("-1");
        entry.setNWlimit("0");
        entry.setComment("PrioriZR");
        entry.setHeight("1000");
        entry.setMainRow(true);
        HashMap<Date, String> values = new HashMap<>();
        values.put(Date.from(Instant.now().minus(Duration.ofMinutes(0))), "290");
        values.put(Date.from(Instant.now().minus(Duration.ofMinutes(15))), "292");
        values.put(Date.from(Instant.now().minus(Duration.ofMinutes(30))), "298");
        values.put(Date.from(Instant.now().minus(Duration.ofMinutes(45))), "310");
        entry.getValues().putAll(values);
    }

    @Test
    public void generatesMapWithoutValuesEntry() {
        var result = entry.getInfos();

        assertFalse(result.containsKey("values"));
    }

    @Test
    public void generatesMapWithoutUnsetValuesSourceAndSubLocation() {
        var result = entry.getInfos();

        assertFalse(result.containsKey("source"));
        assertFalse(result.containsKey("subLocation"));
    }
}
