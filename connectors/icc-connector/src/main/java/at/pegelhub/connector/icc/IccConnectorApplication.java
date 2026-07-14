package at.pegelhub.connector.icc;

import at.pegelhub.lib.runtime.ConnectorApplication;

public final class IccConnectorApplication {
    private IccConnectorApplication() {
    }

    public static void main(String[] args) {
        ConnectorApplication.run(args, new IccConnectorModule());
    }
}
