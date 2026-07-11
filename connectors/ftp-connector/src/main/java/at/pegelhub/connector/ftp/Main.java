package at.pegelhub.connector.ftp;

import at.pegelhub.lib.runtime.ConnectorApplication;

public class Main {
    public static void main(String[] args) throws Exception {
        ConnectorApplication.run(args, new FtpConnectorModule());
    }
}
