package at.pegelhub.connector.ftp.fileparsing;

import java.util.Date;
import java.util.Map;

public interface Entry {
    Map<Date, String> getValues();
    Map<String, String> getInfos();
}
