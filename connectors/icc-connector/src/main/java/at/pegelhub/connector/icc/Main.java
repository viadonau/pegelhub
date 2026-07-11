package at.pegelhub.connector.icc;

import at.pegelhub.lib.runtime.ConnectorApplication;

public class Main {
    public static void main(String[] args) {
        ConnectorApplication.run(args, new IccConnectorModule());
    }
}
