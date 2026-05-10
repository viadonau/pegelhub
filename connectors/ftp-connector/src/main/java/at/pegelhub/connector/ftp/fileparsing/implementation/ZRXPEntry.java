package at.pegelhub.connector.ftp.fileparsing.implementation;

import at.pegelhub.connector.ftp.fileparsing.Entry;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ZRXPEntry implements Entry {
    final Map<Date, String> values = new HashMap<>();
    final Map<String, String> infos = new HashMap<>();

    @Override
    public Map<Date, String> getValues() {
        return values;
    }

    @Override
    public Map<String, String> getInfos() {
        return infos;
    }
}
