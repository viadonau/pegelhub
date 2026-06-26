package at.pegelhub.connector.ftp.fileparsing.implementation;

import at.pegelhub.connector.ftp.fileparsing.Entry;
import at.pegelhub.connector.ftp.fileparsing.Parser;
import at.pegelhub.connector.ftp.fileparsing.ParserType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

public class ZRXPParser implements Parser {
    @Override
    public ParserType getType() {
        return ParserType.ZRXP;
    }

    @Override
    public Stream<Entry> parse(InputStream is) throws IOException {
        List<String> lines = null;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            lines = br.lines()
                    .filter(l -> !l.isBlank())
                    .filter(l -> !l.startsWith("##"))
                    .toList();
            br.close();
        }
        if (lines.isEmpty()) {
            return Stream.of();
        }

        List<Entry> entries = new ArrayList<>();
        ZRXPEntry entry = null;
        boolean valueRegion = true;
        for (var line : lines) {
            if (line.startsWith("#")) {
                if (valueRegion) {
                    valueRegion = false;
                    if (entry != null) {
                        entries.add(entry);
                    }
                    entry = new ZRXPEntry();
                }
                parseHeader(entry, line);
                continue;
            }
            valueRegion = true;
            String[] splits = line.split("\\s");
            String dateString = splits[0];
            Date date = deconstructDateString(dateString);
            String value = splits[1];
            assert entry != null;
            entry.values.put(date, value);
        }
        if (entry != null) {
            entries.add(entry);
        }

        return entries.stream();
    }

    private void parseHeader(ZRXPEntry entry, String header) {
        String[] sections = Arrays.stream(header.substring(1).split("(\\|\\*\\|)|(;\\*;)"))
                .filter(s -> !s.isBlank())
                .toArray(String[]::new);
        for (String s : sections) {
            if (s.startsWith("CDASANAME")) {
                entry.infos.put("name", s.substring("CDASANAME".length()));
            } else if (s.startsWith("CUNIT")) {
                entry.infos.put("unit", s.substring("CUNIT".length()));
            } else if (s.startsWith("REXCHANGE")) {
                String[] customSplits = s.substring("REXCHANGE".length()).split("_");
                entry.infos.put("location", customSplits[1]);
                entry.infos.put("parameter", customSplits[2]);
            } else if (s.startsWith("RINVAL")) {
                // ignore
                continue;
            } else {
                throw new IllegalStateException(String.format("Unknown symbol read at start: \"%s\"", s));
            }
        }
    }

    private Date deconstructDateString(String timeString) {
        var format = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return Date.from(LocalDateTime.parse(timeString, format).atZone(ZoneId.of("UTC")).toInstant());
    }
}
