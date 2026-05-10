package at.pegelhub.connector.ftp;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.OutputStream;

public class LogOutputStream extends OutputStream {
    private final Logger log;
    private String line;

    public LogOutputStream(Logger log) {
        this.log = log;
        this.line = "";
    }

    @Override
    public void write(int b) throws IOException {
        char c = (char)(b & 0xFF);
        if (c == '\n') {
            log.info(line);
            line = "";
        } else {
            line += c;
        }
    }
}
