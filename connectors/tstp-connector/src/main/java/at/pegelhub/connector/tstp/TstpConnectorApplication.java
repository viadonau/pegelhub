package at.pegelhub.connector.tstp;

import at.pegelhub.lib.runtime.ConnectorApplication;

public final class TstpConnectorApplication {
    private TstpConnectorApplication() {
    }

    public static void main(String[] args) {
        ConnectorApplication.run(args, new TstpConnectorModule());
    }
}
