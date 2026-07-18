package at.pegelhub.connector.ftp;

import at.pegelhub.lib.runtime.ConnectorApplication;

public final class FtpConnectorApplication {
    private FtpConnectorApplication() {
    }

    public static void main(String[] args) {
        ConnectorApplication.run(args, new FtpConnectorModule());
    }
}
