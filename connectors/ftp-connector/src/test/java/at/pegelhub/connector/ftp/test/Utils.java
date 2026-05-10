package at.pegelhub.connector.ftp.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class Utils {
    public static String getResource(String name) throws IOException {
        try (InputStream is = getResourceStream(name)) {
            if (is == null) {
                return "";
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                return br.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        }
    }

    public static InputStream getResourceStream(String name) {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        return cl.getResourceAsStream(name);
    }
}
