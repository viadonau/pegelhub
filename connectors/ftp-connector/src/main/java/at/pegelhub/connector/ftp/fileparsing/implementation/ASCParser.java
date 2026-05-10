package at.pegelhub.connector.ftp.fileparsing.implementation;

import at.pegelhub.connector.ftp.fileparsing.Entry;
import at.pegelhub.connector.ftp.fileparsing.Parser;
import at.pegelhub.connector.ftp.fileparsing.ParserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.stream.Stream;

public class ASCParser implements Parser {
    private static final Logger LOG = LoggerFactory.getLogger(ASCParser.class);

    @Override
    public ParserType getType() {
        return ParserType.ASC;
    }

    @Override
    public Stream<Entry> parse(InputStream is) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            final StringBuilder sb = new StringBuilder();
            final ArrayList<Entry> parsedEntries = new ArrayList<>();
            br.lines()
                    .filter(l -> !l.isBlank())
                    .forEachOrdered(line -> {
                        if (line.trim().equals("BEGIN")) {
                            if (sb.length() > 0) {
                                try {
                                    ASCEntry entry = ASCParser.parse(sb.toString());
                                    parsedEntries.add(entry);
                                } catch (IllegalArgumentException e) {
                                    LOG.error("Couldn't parse entry!", e);
                                }
                            }
                            sb.setLength(0);
                        }
                        sb.append(line);
                        sb.append(System.lineSeparator());
                    });
            br.close();
            if (sb.length() > 0) {
                parsedEntries.add(ASCParser.parse(sb.toString()));
            }
            return parsedEntries.stream();
        }
    }

    private static ASCEntry parse(String input) throws IllegalArgumentException {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        ASCEntry entry = null;
        boolean valueSegment = false;

        var iter = input.lines().iterator();
        while (iter.hasNext()) {
            String line = iter.next();
            if (line.isBlank()) {
                continue;
            }

            if (line.startsWith("BEGIN")) {
                entry = new ASCEntry();
            } else if (line.startsWith("Parameter:")) {
                assert entry != null;
                entry.setParameter(line.substring("Parameter:".length()).trim());
            } else if (line.startsWith("Ort:")) {
                assert entry != null;
                entry.setLocation(line.substring("Ort:".length()).trim());
            } else if (line.startsWith("SubOrt:")) {
                assert entry != null;
                entry.setSubLocation(line.substring("SubOrt:".length()).trim());
            } else if (line.startsWith("DefArt:")) {
                assert entry != null;
                entry.setDefKind(line.substring("DefArt:".length()).trim());
            } else if (line.startsWith("Herkunft:")) {
                assert entry != null;
                entry.setOrigin(line.substring("Herkunft:".length()).trim());
            } else if (line.startsWith("Quelle:")) {
                assert entry != null;
                entry.setSource(line.substring("Quelle:".length()).trim());
            } else if (line.startsWith("Reihenart:")) {
                assert entry != null;
                entry.setRowKind(line.substring("Reihenart:".length()));
            } else if (line.startsWith("Version:")) {
                assert entry != null;
                entry.setVersion(line.substring("Version:".length()));
            } else if (line.startsWith("X:")) {
                assert entry != null;
                entry.setX(line.substring("X:".length()).trim());
            } else if (line.startsWith("Y:")) {
                assert entry != null;
                entry.setY(line.substring("Y:".length()).trim());
            } else if (line.startsWith("Einheit:")) {
                assert entry != null;
                entry.setUnit(line.substring("Einheit:".length()).trim());
            } else if (line.startsWith("Messgenau:")) {
                assert entry != null;
                entry.setAccuracy(line.substring("Messgenau:".length()).trim());
            } else if (line.startsWith("FToleranz:")) {
                assert entry != null;
                entry.setTolerance(line.substring("FToleranz:".length()).trim());
            } else if (line.startsWith("NWGrenze:")) {
                assert entry != null;
                entry.setNWlimit(line.substring("NWLimit:".length()).trim());
            } else if (line.startsWith("Kommentar:")) {
                assert entry != null;
                entry.setComment(line.substring("Kommentar:".length()).trim());
            } else if (line.startsWith("Hoehe:")) {
                assert entry != null;
                entry.setHeight(line.substring("Hoehe:".length()).trim());
            } else if (line.startsWith("Hauptreihe:")) {
                assert entry != null;
                String value = line.substring("Hauptreihe:".length()).trim();
                entry.setMainRow(value.equals("Ja"));
            } else if (line.startsWith("Werte:")) {
                valueSegment = true;
            } else if (valueSegment && !line.isBlank()) {
                // else it is data
                assert entry != null;
                try {
                    Date date = sdf.parse(line.substring(0, 19));
                    String value = line.substring(19).trim();
                    entry.getValues().put(date, value);
                } catch (Exception e) {
                    throw new IllegalArgumentException(String.format("Unknown line in value segment: %s", line));
                }
            } else {
                // unknown symbol
                throw new IllegalArgumentException(String.format("Unknown symbol encountered on line: %s", line));
            }
        }
        return entry;
    }
}
